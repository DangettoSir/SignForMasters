package ru.cdt.tgbot_nails.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.cdt.tgbot_nails.model.*
import ru.cdt.tgbot_nails.repository.AppointmentRepository
import ru.cdt.tgbot_nails.repository.UserRepository
import java.time.LocalDate
import java.time.LocalTime

/**
 * Сервис для работы с записями и пользователями.
 * Реализует бизнес-логику бронирования, управления пользователями и проверки доступности слотов.
 */
@Service
class AppointmentService(
    private val masterService: MasterService,
    private val appointmentRepository: AppointmentRepository,
    private val userRepository: UserRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val timeSlots = mutableMapOf<String, TimeSlot>()

    /**
     * Регистрирует пользователя в системе.
     */
    fun registerUser(user: User): User? {
        log.info("Registering user: {}", user.id)
        val stored = user.copy(isRegistered = true)
        val result = userRepository.upsert(stored)
        log.info("User {} registration result: {}", user.id, if (result != null) "success" else "failed")
        return result
    }

    /**
     * Получает пользователя по ID.
     */
    fun getUserById(userId: Long): User? {
        log.debug("Getting user by id: {}", userId)
        return userRepository.getById(userId)
    }

    /**
     * Обновляет номер телефона пользователя.
     */
    fun updateUserPhone(userId: Long, phoneNumber: String): User? {
        log.info("Updating phone for user {}: {}", userId, phoneNumber)
        return userRepository.updatePhone(userId, phoneNumber)
    }

    /**
     * Получает доступные временные слоты для мастера на указанную дату.
     */
    fun getAvailableTimeSlots(masterId: String, date: LocalDate): List<TimeSlot> {
        log.debug("Getting available slots for master {} on {}", masterId, date)
        val master = masterService.getMasterById(masterId) ?: return emptyList()
        val dayOfWeek = date.dayOfWeek.name.lowercase()
        val daySchedule = master.schedule[dayOfWeek] ?: return emptyList()

        val slots = mutableListOf<TimeSlot>()
        val startTime = LocalTime.parse(daySchedule.start)
        val endTime = LocalTime.parse(daySchedule.end)
        val slotDuration = 30
        val totalMinutes = java.time.Duration.between(startTime, endTime).toMinutes().toInt()
        val slotsPerDay = if (daySchedule.slots > 0) daySchedule.slots else (totalMinutes / slotDuration)

        var currentTime = startTime
        while (currentTime.isBefore(endTime)) {
            val slotKey = "${masterId}_${date}_${currentTime}"
            val existsAppointment = appointmentRepository.existsForMasterDateTime(masterId, date, currentTime)
            // Скрываем прошедшие слоты на текущую дату (например, сейчас 10:26 → слот 10:00 не показываем)
            val isPastForToday = date.isEqual(LocalDate.now()) && !currentTime.isAfter(LocalTime.now())
            
            // Добавляем слот только если НЕТ существующей записи
            if (!existsAppointment && !isPastForToday) {
                val newSlot = TimeSlot(masterId, date, currentTime, true)
                timeSlots[slotKey] = newSlot
                slots.add(newSlot)
            }
            // Если запись существует - слот НЕ добавляем (он занят)
            
            currentTime = currentTime.plusMinutes(slotDuration.toLong())
        }

        return slots.take(slotsPerDay)
    }

    fun bookAppointment(userId: Long, masterId: String, date: LocalDate, time: LocalTime): Appointment? {
        // Защита: запрещаем бронирование в прошлое время
        if (date.isBefore(LocalDate.now()) || (date.isEqual(LocalDate.now()) && !time.isAfter(LocalTime.now()))) {
            log.warn("Attempt to book past slot: {} {} {}", masterId, date, time)
            return null
        }
        val slotKey = "${masterId}_${date}_${time}"
        if (appointmentRepository.existsForMasterDateTime(masterId, date, time)) return null

        // Переиспользуем отменённую запись того же пользователя (реактивация), чтобы не упираться в PK
        val appointmentId = "${userId}_${masterId}_${date}_${time}"
        if (appointmentRepository.reactivateIfCancelled(appointmentId)) {
            log.info("Reactivated cancelled appointment {}", appointmentId)
            timeSlots[slotKey] = TimeSlot(masterId, date, time, isAvailable = false, userId = userId)
            return appointmentRepository.getById(appointmentId)
        }

        // Иначе создаём новую запись
        val appointment = Appointment(
            id = appointmentId,
            userId = userId,
            masterId = masterId,
            date = date,
            time = time
        )
        if (appointmentRepository.insert(appointment)) {
            timeSlots[slotKey] = TimeSlot(masterId, date, time, isAvailable = false, userId = userId)
            return appointmentRepository.getById(appointmentId)
        }
        return null
    }

    fun getUserAppointments(userId: Long): List<Appointment> =
        appointmentRepository.getByUser(userId).filter { it.status == AppointmentStatus.CONFIRMED }

    /**
     * Отменяет запись, освобождая слот для повторной записи.
     */
    fun cancelAppointment(appointmentId: String): Boolean {
        log.info("Cancelling appointment: {}", appointmentId)
        val appt = appointmentRepository.getById(appointmentId)
        if (appt == null) {
            log.warn("Appointment {} not found", appointmentId)
            return false
        }
        val ok = appointmentRepository.cancel(appointmentId)
        if (ok) {
            log.info("Appointment {} cancelled successfully", appointmentId)
            val slotKey = "${appt.masterId}_${appt.date}_${appt.time}"
            timeSlots[slotKey] = TimeSlot(appt.masterId, appt.date, appt.time, isAvailable = true, userId = null)
        } else {
            log.warn("Failed to cancel appointment {}", appointmentId)
        }
        return ok
    }

    fun getAllAppointments(): List<Appointment> = appointmentRepository.getAll()

    fun getAppointmentById(appointmentId: String): Appointment? = appointmentRepository.getById(appointmentId)
}
