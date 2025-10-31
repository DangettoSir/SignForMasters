package ru.cdt.tgbot_nails.controller.handlers

import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.cdt.tgbot_nails.controller.UserState
import ru.cdt.tgbot_nails.keyboard.KeyboardFactory
import ru.cdt.tgbot_nails.model.User
import ru.cdt.tgbot_nails.service.AdminService
import ru.cdt.tgbot_nails.service.AppointmentService

/**
 * Обработчик регистрации пользователей и их первой настройки.
 * Управляет командой /start и запросом контакта.
 */
class UserRegistrationHandler(
    private val bot: TelegramLongPollingBot,
    private val appointmentService: AppointmentService,
    private val adminService: AdminService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    /**
     * Обрабатывает команду /start - регистрация и приветствие пользователя.
     */
    fun handleStart(
        chatId: Long,
        userId: Long,
        firstName: String,
        lastName: String?,
        username: String?,
        setState: (UserState) -> Unit
    ) {
        try {
            log.info("Starting registration for user {}: {} {}", userId, firstName, lastName ?: "")
            val user = User(userId, firstName, lastName, username)
            appointmentService.registerUser(user)
            log.info("User {} registered successfully", userId)
            
            if (adminService.isAdmin(userId)) {
                val message = """
                    👋 Добро пожаловать в салон красоты!
                    
                    Оу, я вижу что вы админ!
                """.trimIndent()
                sendMessage(chatId, message, KeyboardFactory.createAdminMainMenuKeyboard())
            } else {
                val message = """
                    👋 Добро пожаловать в салон красоты!
                    
                    Для записи на прием необходимо поделиться номером телефона.
                    Нажмите кнопку ниже:
                """.trimIndent()
                sendMessage(chatId, message, KeyboardFactory.createPhoneRequestKeyboard())
                setState(UserState.WAITING_PHONE)
            }
        } catch (e: Exception) {
            log.error("Error during user {} registration", userId, e)
            sendMessage(chatId, "Произошла ошибка при регистрации. Попробуйте позже.")
        }
    }
    
    /**
     * Обрабатывает получение контакта (номер телефона) от пользователя.
     */
    fun handleContact(
        chatId: Long,
        userId: Long,
        contactUserId: Long?,
        phoneNumber: String,
        clearState: () -> Unit
    ) {
        log.info("Received contact from user {}: {}", userId, phoneNumber)
        
        if (contactUserId == userId) {
            appointmentService.updateUserPhone(userId, phoneNumber)
            sendMessage(chatId, "✅ Номер телефона сохранен! Теперь вы можете записаться на прием.", KeyboardFactory.createMainMenuKeyboard())
            clearState()
            log.info("Phone number saved for user {}", userId)
        } else {
            log.warn("Contact userId mismatch for user {}", userId)
        }
    }
    
    /**
     * Обрабатывает запрос информации о салоне.
     */
    fun handleInfo(chatId: Long, userId: Long) {
        try {
            val isAdmin = adminService.isAdmin(userId)
            val message = "Информация и команды. Выберите нужную команду на клавиатуре ниже."
            sendMessage(chatId, message, KeyboardFactory.createInfoCommandsKeyboard(isAdmin))
        } catch (e: Exception) {
            sendMessage(chatId, "Произошла ошибка.")
        }
    }
    
    private fun sendMessage(chatId: Long, text: String, keyboard: org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup? = null) {
        try {
            val builder = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
            if (keyboard != null) {
                builder.replyMarkup(keyboard)
            }
            bot.execute(builder.build())
        } catch (e: Exception) {
            log.error("Failed to send message", e)
        }
    }
    
    private fun sendMessage(chatId: Long, text: String) {
        try {
            val message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build()
            bot.execute(message)
        } catch (e: Exception) {
            log.error("Failed to send message", e)
        }
    }
}

