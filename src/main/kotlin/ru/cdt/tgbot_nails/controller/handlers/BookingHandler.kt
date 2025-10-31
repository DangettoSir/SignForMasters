package ru.cdt.tgbot_nails.controller.handlers

import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import ru.cdt.tgbot_nails.controller.BookingData
import ru.cdt.tgbot_nails.controller.UserState
import ru.cdt.tgbot_nails.keyboard.KeyboardFactory
import ru.cdt.tgbot_nails.model.Master
import ru.cdt.tgbot_nails.service.AppointmentService
import ru.cdt.tgbot_nails.service.MasterService
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Обработчик логики бронирования записей к мастерам.
 * Управляет выбором мастера, даты, времени и подтверждением бронирования.
 */
class BookingHandler(
    private val bot: TelegramLongPollingBot,
    private val appointmentService: AppointmentService,
    private val masterService: MasterService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    /**
     * Обрабатывает инициализацию процесса бронирования.
     * Проверяет наличие телефона у пользователя и показывает список мастеров.
     */
    fun handleBookAppointment(
        chatId: Long,
        userId: Long,
        hasPhone: Boolean,
        setState: (UserState) -> Unit,
        setBookingMessageId: (Int) -> Unit,
        getBookingMessageId: () -> Int?
    ): Boolean {
        log.info("User {} started booking appointment", userId)
        try {
            if (!hasPhone) {
                return false
            }
            
            val masters = masterService.getAllMasters()
            if (masters.isEmpty()) {
                sendMessage(chatId, "К сожалению, сейчас нет доступных мастеров.")
                return true
            }
            
            val msgId = getBookingMessageId()
            if (msgId != null) {
                editMessage(chatId, msgId, "Выберите мастера:", KeyboardFactory.createMastersKeyboard(masters))
            } else {
                val sent = sendMessage(chatId, "Выберите мастера:", KeyboardFactory.createMastersKeyboard(masters))
                setBookingMessageId(sent.messageId)
            }
            setState(UserState.SELECTING_MASTER)
            log.info("Sent master list to user {}", userId)
            return true
        } catch (e: Exception) {
            log.error("Error booking appointment for user {}", userId, e)
            val msgId = getBookingMessageId()
            if (msgId != null) {
                editMessage(chatId, msgId, "Произошла ошибка при записи на прием.")
            } else {
                sendMessage(chatId, "Произошла ошибка при записи на прием.")
            }
            return true
        }
    }
    
    /**
     * Обрабатывает выбор мастера пользователем.
     */
    fun handleMasterSelection(
        chatId: Long,
        userId: Long,
        masterId: String,
        setBookingData: (BookingData) -> Unit,
        setWeekOffset: (Int) -> Unit,
        setState: (UserState) -> Unit,
        getBookingMessageId: () -> Int?,
        setBookingMessageId: (Int) -> Unit
    ) {
        log.info("User {} selected master: {}", userId, masterId)
        
        val master = masterService.getMasterById(masterId)
        if (master == null) {
            log.warn("Master {} not found", masterId)
            val msgId = getBookingMessageId()
            if (msgId != null) {
                editMessage(chatId, msgId, "Мастер не найден.")
            } else {
                sendMessage(chatId, "Мастер не найден.")
            }
            return
        }
        
        setBookingData(BookingData(masterId = masterId))
        setWeekOffset(0)
        val dates = computeAllowedDates(master)
        log.info("Generated {} available dates for master {}", dates.size, masterId)
        
        val msgId = getBookingMessageId()
        if (msgId != null) {
            editMessage(chatId, msgId, "Выберите дату:", KeyboardFactory.createDatesKeyboard(dates, 0))
        } else {
            val sent = sendMessage(chatId, "Выберите дату:", KeyboardFactory.createDatesKeyboard(dates, 0))
            setBookingMessageId(sent.messageId)
        }
        setState(UserState.SELECTING_DATE)
    }
    
    /**
     * Обрабатывает выбор даты для записи.
     */
    fun handleDateSelection(
        chatId: Long,
        userId: Long,
        dateStr: String,
        booking: BookingData,
        setBookingData: (BookingData) -> Unit,
        setState: (UserState) -> Unit,
        getBookingMessageId: () -> Int?,
        setBookingMessageId: (Int) -> Unit
    ) {
        log.info("User {} selecting date: {}", userId, dateStr)
        val date = try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
            log.error("Date parsing error for user {}: {}", userId, dateStr, e)
            sendMessage(chatId, "Неверный формат даты.")
            return
        }
        
        val master = masterService.getMasterById(booking.masterId)
        val dow = date.dayOfWeek.name.lowercase()
        log.debug("Validating date {} (dow: {}) for master {}", date, dow, booking.masterId)
        
        if (master == null || !master.schedule.containsKey(dow)) {
            log.warn("No schedule for master {} on {}", booking.masterId, dow)
            val msgId = getBookingMessageId()
            if (msgId != null) {
                editMessage(chatId, msgId, "На выбранную дату нет доступных слотов.")
            } else {
                sendMessage(chatId, "На выбранную дату нет доступных слотов.")
            }
            return
        }
        
        val timeSlots = try {
            appointmentService.getAvailableTimeSlots(booking.masterId, date)
        } catch (e: Exception) {
            log.error("Error getting time slots for user {}, master {}, date {}", userId, booking.masterId, date, e)
            val msgId = getBookingMessageId()
            if (msgId != null) {
                editMessage(chatId, msgId, "Ошибка при получении слотов: ${e.message}")
            } else {
                sendMessage(chatId, "Ошибка при получении слотов: ${e.message}")
            }
            return
        }
        
        if (timeSlots.isEmpty()) {
            log.info("No available time slots for master {} on {}", booking.masterId, date)
            val msgId = getBookingMessageId()
            if (msgId != null) {
                editMessage(chatId, msgId, "На выбранную дату нет доступных слотов.")
            } else {
                sendMessage(chatId, "На выбранную дату нет доступных слотов.")
            }
            return
        }
        
        log.info("User {} has {} available time slots for {}", userId, timeSlots.size, date)
        setBookingData(booking.copy(selectedDate = date))
        val msgId = getBookingMessageId()
        if (msgId != null) {
            editMessage(chatId, msgId, "Выберите время:", KeyboardFactory.createTimeSlotsKeyboard(timeSlots))
        } else {
            val sent = sendMessage(chatId, "Выберите время:", KeyboardFactory.createTimeSlotsKeyboard(timeSlots))
            setBookingMessageId(sent.messageId)
        }
        setState(UserState.SELECTING_TIME)
    }
    
    /**
     * Обрабатывает выбор времени для записи.
     */
    fun handleTimeSelection(
        chatId: Long,
        userId: Long,
        timeStr: String,
        booking: BookingData,
        setBookingData: (BookingData) -> Unit,
        setState: (UserState) -> Unit,
        getBookingMessageId: () -> Int?,
        setBookingMessageId: (Int) -> Unit
    ) {
        val time = LocalTime.parse(timeStr)
        setBookingData(booking.copy(selectedTime = time))
        
        val master = masterService.getMasterById(booking.masterId)
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        
        val message = """
            📋 Подтвердите запись:
            
            Мастер: ${master?.name}
            Специализация: ${master?.specialization}
            Дата: ${booking.selectedDate?.format(formatter)}
            Время: ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}
        """.trimIndent()
        
        val msgId = getBookingMessageId()
        if (msgId != null) {
            editMessage(chatId, msgId, message, KeyboardFactory.createConfirmationKeyboard())
        } else {
            val sent = sendMessage(chatId, message, KeyboardFactory.createConfirmationKeyboard())
            setBookingMessageId(sent.messageId)
        }
        setState(UserState.CONFIRMING_BOOKING)
    }
    
    /**
     * Обрабатывает подтверждение бронирования.
     */
    fun handleConfirmBooking(
        chatId: Long,
        userId: Long,
        booking: BookingData,
        clearSession: () -> Unit,
        getBookingMessageId: () -> Int?,
        isAdmin: () -> Boolean
    ) {
        if (booking.selectedDate == null || booking.selectedTime == null) {
            val msgId = getBookingMessageId()
            if (msgId != null) {
                editMessage(chatId, msgId, "Ошибка при создании записи.")
            } else {
                sendMessage(chatId, "Ошибка при создании записи.")
            }
            return
        }
        
        val appointment = appointmentService.bookAppointment(userId, booking.masterId, booking.selectedDate!!, booking.selectedTime!!)
        
        if (appointment != null) {
            val master = masterService.getMasterById(booking.masterId)
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            
            val message = """
                ✅ Запись успешно создана!
                
                Мастер: ${master?.name}
                Дата: ${booking.selectedDate.format(formatter)}
                Время: ${booking.selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))}
                
                Спасибо за выбор нашего салона!
            """.trimIndent()
            
            val msgId = getBookingMessageId()
            val menuKeyboard = if (isAdmin()) KeyboardFactory.createAdminMainMenuKeyboard() else KeyboardFactory.createMainMenuKeyboard()
            if (msgId != null) {
                editMessage(chatId, msgId, message)
                sendMessageReplyKeyboard(chatId, "Главное меню:", menuKeyboard)
            } else {
                sendMessageReplyKeyboard(chatId, message, menuKeyboard)
            }
        } else {
            val msgId = getBookingMessageId()
            if (msgId != null) {
                editMessage(chatId, msgId, "К сожалению, этот слот уже занят. Выберите другое время.")
            } else {
                sendMessage(chatId, "К сожалению, этот слот уже занят. Выберите другое время.")
            }
        }
        
        clearSession()
    }
    
    /**
     * Обрабатывает отмену бронирования.
     */
    fun handleCancelBooking(
        chatId: Long,
        userId: Long,
        clearSession: () -> Unit,
        getBookingMessageId: () -> Int?
    ) {
        val msgId = getBookingMessageId()
        if (msgId != null) {
            editMessage(chatId, msgId, "Запись отменена.")
            sendMessageReplyKeyboard(chatId, "Главное меню:", KeyboardFactory.createMainMenuKeyboard())
        } else {
            sendMessageReplyKeyboard(chatId, "Запись отменена.", KeyboardFactory.createMainMenuKeyboard())
        }
        clearSession()
    }
    
    /**
     * Обрабатывает навигацию по неделям при выборе даты.
     */
    fun handleWeekNavigation(
        chatId: Long,
        userId: Long,
        weekOffset: Int,
        booking: BookingData,
        setWeekOffset: (Int) -> Unit,
        getBookingMessageId: () -> Int?,
        setBookingMessageId: (Int) -> Unit
    ) {
        val master = masterService.getMasterById(booking.masterId) ?: return
        
        setWeekOffset(weekOffset)
        val dates = computeAllowedDates(master)
        
        val msgId = getBookingMessageId()
        if (msgId != null) {
            editMessage(chatId, msgId, "Выберите дату:", KeyboardFactory.createDatesKeyboard(dates, weekOffset))
        } else {
            val sent = sendMessage(chatId, "Выберите дату:", KeyboardFactory.createDatesKeyboard(dates, weekOffset))
            setBookingMessageId(sent.messageId)
        }
    }
    
    private fun computeAllowedDates(master: Master): List<LocalDate> {
        val allowed = mutableListOf<LocalDate>()
        val today = LocalDate.now()
        for (i in 0..27) {
            val d = today.plusDays(i.toLong())
            val dowKey = d.dayOfWeek.name.lowercase()
            if (master.schedule.containsKey(dowKey)) {
                allowed.add(d)
            }
        }
        return if (allowed.isNotEmpty()) allowed else listOf(today)
    }
    
    private fun sendMessage(chatId: Long, text: String, keyboard: InlineKeyboardMarkup? = null): org.telegram.telegrambots.meta.api.objects.Message {
        val builder = org.telegram.telegrambots.meta.api.methods.send.SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
        if (keyboard != null) {
            builder.replyMarkup(keyboard)
        }
        return bot.execute(builder.build())
    }
    
    private fun sendMessageReplyKeyboard(chatId: Long, text: String, keyboard: org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup) {
        val message = org.telegram.telegrambots.meta.api.methods.send.SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .replyMarkup(keyboard)
            .build()
        bot.execute(message)
    }
    
    private fun editMessage(chatId: Long, messageId: Int, text: String, keyboard: InlineKeyboardMarkup? = null) {
        try {
            val edit = EditMessageText()
            edit.chatId = chatId.toString()
            edit.messageId = messageId
            edit.text = text
            if (keyboard != null) {
                edit.replyMarkup = keyboard
            }
            bot.execute(edit)
        } catch (e: Exception) {
            log.warn("Failed to edit message: ${e.message}")
            if (keyboard != null) {
                sendMessage(chatId, text, keyboard)
            } else {
                sendMessage(chatId, text)
            }
        }
    }
    
    private fun sendMessage(chatId: Long, text: String) {
        val message = org.telegram.telegrambots.meta.api.methods.send.SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .build()
        bot.execute(message)
    }
}

