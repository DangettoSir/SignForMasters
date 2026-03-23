package ru.cdt.tgbot_nails.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.cdt.tgbot_nails.model.AppointmentStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Сервис напоминаний о предстоящих записях.
 * Отправляет уведомления за 1 день, 5 часов и 1 час до сеанса.
 */
@Service
class ReminderService(
    private val appointmentService: AppointmentService,
    private val masterService: MasterService,
    private val masterNotificationService: MasterNotificationService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    /**
     * Планировщик: проверяет напоминания каждые 30 минут.
     */
    @Scheduled(fixedRate = 1800000)
    fun checkReminders() {
        try {
            log.debug("Checking reminders...")
            val now = LocalDateTime.now()
            val appointments = appointmentService.getAllAppointments()
                .filter { it.status == AppointmentStatus.CONFIRMED }
            
            appointments.forEach { appointment ->
                checkAppointmentReminders(appointment, now)
            }
        } catch (e: Exception) {
            log.error("Error checking reminders", e)
        }
    }
    
    /**
     * Проверяет напоминания для конкретной записи.
     */
    private fun checkAppointmentReminders(appointment: ru.cdt.tgbot_nails.model.Appointment, now: LocalDateTime) {
        val appointmentDateTime = LocalDateTime.of(appointment.date, appointment.time)
        val timeUntilAppointment = java.time.Duration.between(now, appointmentDateTime)
        
        if (timeUntilAppointment.isNegative) {
            handleMissedAppointment(appointment)
            return
        }
        
        val hoursUntil = timeUntilAppointment.toHours()
        val daysUntil = timeUntilAppointment.toDays()
        
        when {
            daysUntil == 1L && hoursUntil in 20..24 -> {
                sendReminder(appointment, "1 день", "завтра")
            }
            hoursUntil == 5L -> {
                sendReminder(appointment, "5 часов", "через 5 часов")
            }
            hoursUntil == 1L -> {
                sendReminder(appointment, "1 час", "через 1 час")
            }
        }
    }
    
    /**
     * Отправляет напоминание пользователю.
     */
    private fun sendReminder(appointment: ru.cdt.tgbot_nails.model.Appointment, timeText: String, timeDescription: String) {
        try {
            val master = masterService.getMasterById(appointment.masterId)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            
            val appointmentDateTime = java.time.LocalDateTime.of(appointment.date, appointment.time)
            val message = """
                🔔 Напоминание о записи
                
                У вас запись ${timeDescription}:
                
                Мастер: ${master?.name ?: "Неизвестно"}
                Специализация: ${master?.specialization ?: ""}
                Дата и время: ${appointmentDateTime.format(formatter)}
                
                Не забудьте прийти на сеанс!
            """.trimIndent()
            

            log.info("Reminder for user {}: {}", appointment.userId, message)
            

            master?.telegramId?.let { masterTelegramId ->
                masterNotificationService.sendUpcomingAppointmentNotification(
                    masterTelegramId, 
                    appointment, 
                    timeText
                )
            }
            
        } catch (e: Exception) {
            log.error("Error sending reminder for appointment {}", appointment.id, e)
        }
    }
    
    /**
     * Обрабатывает пропущенный сеанс.
     */
    private fun handleMissedAppointment(appointment: ru.cdt.tgbot_nails.model.Appointment) {
        try {
            log.info("Appointment {} was missed, cancelling", appointment.id)
            

            appointmentService.cancelAppointment(appointment.id)
            

            val master = masterService.getMasterById(appointment.masterId)
            master?.telegramId?.let { masterTelegramId ->
                masterNotificationService.sendMissedAppointmentNotification(masterTelegramId, appointment)
            }
            
        } catch (e: Exception) {
            log.error("Error handling missed appointment {}", appointment.id, e)
        }
    }
}
