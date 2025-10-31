package ru.cdt.tgbot_nails.controller

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import ru.cdt.tgbot_nails.config.TelegramBotConfig
import ru.cdt.tgbot_nails.controller.handlers.AdminHandler
import ru.cdt.tgbot_nails.controller.handlers.AppointmentHandler
import ru.cdt.tgbot_nails.controller.handlers.BookingHandler
import ru.cdt.tgbot_nails.controller.handlers.UserRegistrationHandler
import ru.cdt.tgbot_nails.controller.state.UserSessionManager
import ru.cdt.tgbot_nails.keyboard.KeyboardFactory
import ru.cdt.tgbot_nails.service.*

/**
 * Главный контроллер Telegram-бота для записи на маникюр/педикюр.
 * Выполняет маршрутизацию обновлений к соответствующим обработчикам.
 */
@Component
class NailsBotController(
    @org.springframework.beans.factory.annotation.Qualifier("telegramBotConfig") private val botConfig: TelegramBotConfig,
    private val appointmentService: AppointmentService,
    private val masterService: MasterService,
    private val adminService: AdminService
) : TelegramLongPollingBot() {
    
    private val log = LoggerFactory.getLogger(javaClass)
    private val sessionManager = UserSessionManager()
    
    private val bookingHandler = BookingHandler(this, appointmentService, masterService)
    private val appointmentHandler = AppointmentHandler(this, appointmentService, masterService)
    private val registrationHandler = UserRegistrationHandler(this, appointmentService, adminService)
    private val adminHandler = AdminHandler(this, adminService, masterService, appointmentService)
    
    override fun getBotUsername(): String = botConfig.username
    
    override fun getBotToken(): String = botConfig.token
    
    /**
     * Точка входа для всех обновлений от Telegram API.
     * Маршрутизирует обновления по типу: текстовые сообщения, контакты, callback-запросы.
     */
    override fun onUpdateReceived(update: Update) {
        try {
            log.debug("Received update: {}", update.updateId)
            when {
                update.hasMessage() && update.message.hasText() -> {
                    log.debug("Processing text message from user: {}", update.message.from.id)
                    handleMessage(update)
                }
                update.hasMessage() && update.message.hasContact() -> {
                    log.debug("Processing contact from user: {}", update.message.from.id)
                    handleContact(update)
                }
                update.hasCallbackQuery() -> {
                    log.debug("Processing callback query: {} from user: {}", update.callbackQuery.data, update.callbackQuery.from.id)
                    handleCallbackQuery(update)
                }
            }
        } catch (e: Exception) {
            log.error("Error processing update: ${update.updateId}", e)
        }
    }
    
    /**
     * Обрабатывает текстовые сообщения от пользователя.
     * Маршрутизирует команды и состояния пользователя.
     */
    private fun handleMessage(update: Update) {
        val message = update.message
        val chatId = message.chatId
        val userId = message.from.id
        val text = message.text
        
        log.info("Message from user {}: {}", userId, text)
        
        val currentState = sessionManager.getState(userId)
        if (currentState != null) {
            log.debug("User {} in state: {}", userId, currentState)
            when (currentState) {
                UserState.WAITING_ADD_MASTER -> {
                    if (adminService.isAdmin(userId)) {
                        adminHandler.handleAdminAddMasterInput(chatId, userId, text) {
                            sessionManager.setState(userId, null)
                        }
                        return
                    }
                }
                UserState.WAITING_DELETE_MASTER -> {
                        return
                }
                UserState.WAITING_MASTER_NAME -> {
                    if (adminService.isAdmin(userId)) {
                        adminHandler.handleAdminMasterName(chatId, userId, text,
                            { sessionManager.getAdminDraft(userId) },
                            { draft -> sessionManager.setAdminDraft(userId, draft) },
                            { state -> sessionManager.setState(userId, state) },
                            { sessionManager.getAdminMessageId(userId) }
                        )
                        return
                    }
                }
                UserState.WAITING_MASTER_SCHEDULE_DAYS -> {
                    if (adminService.isAdmin(userId)) {
                        adminHandler.handleAdminMasterDays(chatId, userId, text,
                            { sessionManager.getAdminDraft(userId) },
                            { draft -> sessionManager.setAdminDraft(userId, draft) },
                            { state -> sessionManager.setState(userId, state) },
                            { sessionManager.getAdminMessageId(userId) }
                        )
                        return
                    }
                }
                UserState.WAITING_MASTER_SCHEDULE_TIME -> {
                    if (adminService.isAdmin(userId)) {
                        adminHandler.handleAdminMasterTime(chatId, userId, text,
                            { sessionManager.getAdminDraft(userId) },
                            { draft -> sessionManager.setAdminDraft(userId, draft) },
                            { state -> sessionManager.setState(userId, state) },
                            { sessionManager.getAdminMessageId(userId) }
                        )
                        return
                    }
                }
                UserState.WAITING_MASTER_SLOT_DURATION -> {
                    if (adminService.isAdmin(userId)) {
                        adminHandler.handleAdminMasterSlot(chatId, userId, text,
                            { sessionManager.getAdminDraft(userId) },
                            { draft -> sessionManager.setAdminDraft(userId, draft) },
                            { state -> sessionManager.setState(userId, state) },
                            { sessionManager.getAdminMessageId(userId) }
                        )
                        return
                    }
                }
                UserState.WAITING_MASTER_TELEGRAM_ID -> {
                    if (adminService.isAdmin(userId)) {
                        adminHandler.handleAdminMasterTelegramId(chatId, userId, text,
                            { sessionManager.getAdminDraft(userId) },
                            { sessionManager.setAdminDraft(userId, null) },
                            { sessionManager.setState(userId, null) },
                            { sessionManager.getAdminMessageId(userId) }
                        )
                        return
                    }
                }
                else -> log.debug("State {} not handled in message handler", currentState)
            }
        }
        
        when {
            text == "/start" -> {
                log.info("User {} executed /start command", userId)
                registrationHandler.handleStart(chatId, userId, message.from.firstName, message.from.lastName, message.from.userName) { state ->
                    sessionManager.setState(userId, state)
                }
            }
            text == "/admin" -> {
                log.info("User {} executed /admin command", userId)
                adminHandler.handleAdminCommand(chatId, userId,
                    { sessionManager.getAdminMessageId(userId) },
                    { msgId -> sessionManager.setAdminMessageId(userId, msgId) }
                )
            }
            text.startsWith("/add_admin") && adminService.isAdmin(userId) -> adminHandler.handleAddAdmin(chatId, text)
            text.startsWith("/remove_admin") && adminService.isAdmin(userId) -> adminHandler.handleRemoveAdmin(chatId, text)
            text == "📅 Записаться на прием" -> {
                val user = appointmentService.getUserById(userId)
                val hasPhone = adminService.isAdmin(userId) || user?.phoneNumber != null
                
                if (!hasPhone) {
                    sendMessage(chatId, "Сначала поделитесь номером телефона для записи.", KeyboardFactory.createPhoneRequestKeyboard())
                    sessionManager.setState(userId, UserState.WAITING_PHONE)
                } else {
                    bookingHandler.handleBookAppointment(chatId, userId, hasPhone,
                        { state -> sessionManager.setState(userId, state) },
                        { msgId -> sessionManager.setBookingMessageId(userId, msgId) },
                        { sessionManager.getBookingMessageId(userId) }
                    )
                }
            }
            text == "📋 Мои записи" -> appointmentHandler.handleMyAppointments(chatId, userId,
                { sessionManager.getBookingMessageId(userId) },
                { msgId -> sessionManager.setBookingMessageId(userId, msgId) }
            )
            text == "ℹ️ Информация" || text == "/info" -> registrationHandler.handleInfo(chatId, userId)
            text == "🔧 Админ панель" && adminService.isAdmin(userId) -> adminHandler.handleAdminPanel(chatId,
                { sessionManager.getAdminMessageId(userId) },
                { msgId -> sessionManager.setAdminMessageId(userId, msgId) }
            )
            else -> {
                log.warn("Unknown command from user {}: {}", userId, text)
                handleUnknownCommand(chatId)
            }
        }
    }
    
    /**
     * Обрабатывает получение контакта (номер телефона) от пользователя.
     */
    private fun handleContact(update: Update) {
        val message = update.message
        val chatId = message.chatId
        val userId = message.from.id
        val contact = message.contact
        
        registrationHandler.handleContact(chatId, userId, contact.userId, contact.phoneNumber) {
            sessionManager.setState(userId, null)
        }
    }
    
    /**
     * Обрабатывает callback-запросы от inline-кнопок.
     * Маршрутизирует по префиксу данных callback.
     */
    private fun handleCallbackQuery(update: Update) {
        val callbackQuery = update.callbackQuery
        val chatId = callbackQuery.message.chatId
        val userId = callbackQuery.from.id
        val data = callbackQuery.data
        val messageId = callbackQuery.message.messageId
        
        if (adminService.isAdmin(userId)) {
            sessionManager.setAdminMessageId(userId, messageId)
        } else {
            sessionManager.setBookingMessageId(userId, messageId)
        }
        
        log.info("Callback from user {}: {}", userId, data)
        
        when {
            data.startsWith("master_") -> {
                log.debug("Handling master selection")
                val booking = sessionManager.getBookingData(userId) ?: BookingData("")
                val masterId = data.substringAfter("master_")
                bookingHandler.handleMasterSelection(chatId, userId, masterId,
                    { bookingData -> sessionManager.setBookingData(userId, bookingData) },
                    { offset -> sessionManager.setWeekOffset(userId, offset) },
                    { state -> sessionManager.setState(userId, state) },
                    { sessionManager.getBookingMessageId(userId) },
                    { msgId -> sessionManager.setBookingMessageId(userId, msgId) }
                )
            }
            data.startsWith("date_") -> {
                log.debug("Handling date selection")
                val booking = sessionManager.getBookingData(userId) ?: return
                val dateStr = data.substringAfter("date_")
                bookingHandler.handleDateSelection(chatId, userId, dateStr, booking,
                    { bookingData -> sessionManager.setBookingData(userId, bookingData) },
                    { state -> sessionManager.setState(userId, state) },
                    { sessionManager.getBookingMessageId(userId) },
                    { msgId -> sessionManager.setBookingMessageId(userId, msgId) }
                )
            }
            data.startsWith("week_") -> {
                val weekOffset = data.removePrefix("week_").toIntOrNull() ?: 0
                log.debug("Handling week navigation: offset {}", weekOffset)
                val booking = sessionManager.getBookingData(userId) ?: return
                bookingHandler.handleWeekNavigation(chatId, userId, weekOffset, booking,
                    { offset -> sessionManager.setWeekOffset(userId, offset) },
                    { sessionManager.getBookingMessageId(userId) },
                    { msgId -> sessionManager.setBookingMessageId(userId, msgId) }
                )
            }
            data.startsWith("time_") -> {
                log.debug("Handling time selection")
                val booking = sessionManager.getBookingData(userId) ?: return
                val timeStr = data.substringAfter("time_")
                bookingHandler.handleTimeSelection(chatId, userId, timeStr, booking,
                    { bookingData -> sessionManager.setBookingData(userId, bookingData) },
                    { state -> sessionManager.setState(userId, state) },
                    { sessionManager.getBookingMessageId(userId) },
                    { msgId -> sessionManager.setBookingMessageId(userId, msgId) }
                )
            }
            data == "no_slots" -> {
                sendMessage(chatId, "❌ На выбранную дату нет доступных слотов. Выберите другую дату.")
            }
            data == "confirm_booking" -> {
                val booking = sessionManager.getBookingData(userId) ?: return
                bookingHandler.handleConfirmBooking(chatId, userId, booking,
                    { sessionManager.clearUserSession(userId) },
                    { sessionManager.getBookingMessageId(userId) },
                    { adminService.isAdmin(userId) }
                )
            }
            data == "cancel_booking" -> {
                bookingHandler.handleCancelBooking(chatId, userId,
                    { sessionManager.clearUserSession(userId) },
                    { sessionManager.getBookingMessageId(userId) }
                )
            }
            data == "admin_masters" -> adminHandler.editToAdminMasters(chatId, messageId)
            data == "admin_stats" -> adminHandler.handleAdminStats(chatId,
                { sessionManager.getAdminMessageId(userId) }
            )
            data == "admin_add_master" -> {
                adminHandler.editToAdminAddMasterCategory(chatId, userId, messageId,
                    { draft -> sessionManager.setAdminDraft(userId, draft) },
                    { state -> sessionManager.setState(userId, state) }
                )
            }
            data == "admin_edit_master" -> adminHandler.handleAdminEditMaster(chatId, userId)
            data == "admin_delete_master" -> {
                adminHandler.editToAdminDeleteMaster(chatId, userId, messageId) { state ->
                    sessionManager.setState(userId, state)
                }
            }
            data.startsWith("del_page_") -> {
                val page = data.removePrefix("del_page_").toIntOrNull() ?: 0
                adminHandler.handleDeleteMastersPage(chatId, userId, messageId, page)
            }
            data.startsWith("del_pick_") -> {
                val masterId = data.removePrefix("del_pick_")
                adminHandler.handleDeleteMasterPick(chatId, userId, messageId, masterId)
            }
            data == "admin_back" -> adminHandler.handleAdminBack(chatId,
                { sessionManager.getAdminMessageId(userId) }
            )
            data.startsWith("cancel_appointment_") -> {
                val appointmentId = data.substringAfter("cancel_appointment_")
                appointmentHandler.handleCancelAppointment(chatId, userId, appointmentId,
                    { sessionManager.getBookingMessageId(userId) }
                )
            }
            data == "back_to_main" -> handleBackToMain(chatId)
            data == "back" -> handleBack(chatId, userId)
            data.startsWith("cat_") -> {
                val category = data.removePrefix("cat_")
                adminHandler.handleAdminCategorySelected(chatId, userId, messageId, category,
                    { sessionManager.getAdminDraft(userId) },
                    { draft -> sessionManager.setAdminDraft(userId, draft) },
                    { state -> sessionManager.setState(userId, state) }
                )
            }
            else -> {
                log.warn("Unknown callback from user {}: {}", userId, data)
                handleUnknownCallback(chatId)
            }
        }
    }
    
    private fun handleBackToMain(chatId: Long) {
        try {
            // Определяем пользователя по chatId == userId (для приватных чатов)
            val userId = chatId
            val keyboard = if (adminService.isAdmin(userId)) {
                KeyboardFactory.createAdminMainMenuKeyboard()
            } else {
                KeyboardFactory.createMainMenuKeyboard()
            }
            sendMessage(chatId, "Главное меню", keyboard)
        } catch (e: Exception) {
            sendMessage(chatId, "Произошла ошибка.")
        }
    }
    
    private fun handleBack(chatId: Long, userId: Long) {
        try {
            val keyboard = if (adminService.isAdmin(userId)) {
                KeyboardFactory.createAdminMainMenuKeyboard()
            } else {
                KeyboardFactory.createMainMenuKeyboard()
            }
            sendMessage(chatId, "Главное меню", keyboard)
            sessionManager.clearUserSession(userId)
        } catch (e: Exception) {
            sendMessage(chatId, "Произошла ошибка.")
        }
    }
    
    private fun handleUnknownCommand(chatId: Long) {
        try {
            sendMessage(chatId, "Неизвестная команда. Используйте меню для навигации.")
        } catch (e: Exception) {
            log.warn("Failed to send unknown command message: ${e.message}")
        }
    }
    
    private fun handleUnknownCallback(chatId: Long) {
        try {
            sendMessage(chatId, "Неизвестное действие.")
        } catch (e: Exception) {
            log.warn("Failed to send unknown callback message: ${e.message}")
        }
    }
    
    private fun sendMessage(chatId: Long, text: String, keyboard: ReplyKeyboardMarkup? = null) {
        try {
            val message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build()
            
            if (keyboard != null) {
                message.replyMarkup = keyboard
            }
            
            execute(message)
        } catch (e: Exception) {
            log.error("Failed to send message: ${e.message}", e)
        }
    }
}

enum class UserState {
    WAITING_PHONE,
    SELECTING_MASTER,
    SELECTING_DATE,
    SELECTING_TIME,
    CONFIRMING_BOOKING,
    WAITING_ADD_MASTER,
    WAITING_DELETE_MASTER,
    WAITING_MASTER_CATEGORY,
    WAITING_MASTER_NAME,
    WAITING_MASTER_SCHEDULE_DAYS,
    WAITING_MASTER_SCHEDULE_TIME,
    WAITING_MASTER_SLOT_DURATION,
    WAITING_MASTER_TELEGRAM_ID
}

data class BookingData(
    val masterId: String,
    val selectedDate: java.time.LocalDate? = null,
    val selectedTime: java.time.LocalTime? = null
)

data class AdminMasterDraft(
    val category: String? = null,
    val fullName: String? = null,
    val workingDays: String? = null,
    val timeFrom: String? = null,
    val timeTo: String? = null,
    val slotMinutes: Int? = null,
    val telegramId: Long? = null
)
