package ru.cdt.tgbot_nails.controller.handlers

import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.cdt.tgbot_nails.keyboard.KeyboardFactory
import ru.cdt.tgbot_nails.service.AppointmentService
import ru.cdt.tgbot_nails.service.MasterService
import java.time.format.DateTimeFormatter

/**
 * Обработчик логики работы с записями пользователей.
 * Показывает список записей и обрабатывает их отмену.
 */
class AppointmentHandler(
    private val bot: TelegramLongPollingBot,
    private val appointmentService: AppointmentService,
    private val masterService: MasterService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    /**
     * Показывает список активных записей пользователя.
     */
    fun handleMyAppointments(chatId: Long, userId: Long, getBookingMessageId: () -> Int?, setBookingMessageId: (Int) -> Unit) {
        try {
            val appointments = appointmentService.getUserAppointments(userId)
            
            if (appointments.isEmpty()) {
                val msgId = getBookingMessageId()
                if (msgId != null) {
                    editMessage(chatId, msgId, "У вас нет активных записей.")
                } else {
                    sendMessage(chatId, "У вас нет активных записей.")
                }
                return
            }
            
            val msgId = getBookingMessageId()
            if (msgId != null) {
                editMessage(chatId, msgId, "Ваши записи:\n\nВыберите запись для отмены:", KeyboardFactory.createAppointmentsKeyboard(appointments, masterService))
            } else {
                val sent = sendMessageInline(chatId, "Ваши записи:\n\nВыберите запись для отмены:", KeyboardFactory.createAppointmentsKeyboard(appointments, masterService))
                setBookingMessageId(sent.messageId)
            }
        } catch (e: Exception) {
            log.error("Error getting appointments for user {}", userId, e)
            val msgId = getBookingMessageId()
            if (msgId != null) {
                editMessage(chatId, msgId, "Произошла ошибка при получении записей.")
            } else {
                sendMessage(chatId, "Произошла ошибка при получении записей.")
            }
        }
    }
    
    /**
     * Обрабатывает отмену записи пользователем.
     */
    fun handleCancelAppointment(chatId: Long, userId: Long, appointmentId: String, getBookingMessageId: () -> Int?) {
        try {
            val appointment = appointmentService.getAppointmentById(appointmentId)
            
            if (appointment == null || appointment.userId != userId) {
                val msgId = getBookingMessageId()
                if (msgId != null) {
                    editMessage(chatId, msgId, "Запись не найдена.")
                } else {
                    sendMessage(chatId, "Запись не найдена.")
                }
                return
            }
            
            if (appointmentService.cancelAppointment(appointmentId)) {
                val master = masterService.getMasterById(appointment.masterId)
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                
                val message = """
                    ✅ Запись отменена!
                    
                    Мастер: ${master?.name}
                    Дата: ${appointment.date.format(formatter)}
                    Время: ${appointment.time.format(DateTimeFormatter.ofPattern("HH:mm"))}
                """.trimIndent()
                
                val msgId = getBookingMessageId()
                if (msgId != null) {
                    editMessage(chatId, msgId, message)
                    sendMessageReplyKeyboard(chatId, "Главное меню:", KeyboardFactory.createMainMenuKeyboard())
                } else {
                    sendMessage(chatId, message, KeyboardFactory.createMainMenuKeyboard())
                }
            } else {
                val msgId = getBookingMessageId()
                if (msgId != null) {
                    editMessage(chatId, msgId, "Не удалось отменить запись.")
                } else {
                    sendMessage(chatId, "Не удалось отменить запись.")
                }
            }
        } catch (e: Exception) {
            val msgId = getBookingMessageId()
            if (msgId != null) {
                editMessage(chatId, msgId, "Произошла ошибка при отмене записи.")
            } else {
                sendMessage(chatId, "Произошла ошибка при отмене записи.")
            }
        }
    }
    
    private fun sendMessage(chatId: Long, text: String, keyboard: org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup? = null) {
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
    
    private fun sendMessage(chatId: Long, text: String, keyboard: org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup) {
        try {
            val message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .replyMarkup(keyboard)
                .build()
            bot.execute(message)
        } catch (e: Exception) {
            log.error("Failed to send message", e)
        }
    }
    
    private fun sendMessageReplyKeyboard(chatId: Long, text: String, keyboard: org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup) {
        try {
            val message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .replyMarkup(keyboard)
                .build()
            bot.execute(message)
        } catch (e: Exception) {
            log.error("Failed to send message", e)
        }
    }
    
    private fun editMessage(chatId: Long, messageId: Int, text: String, keyboard: org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup? = null) {
        try {
            val edit = org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText()
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
    
    private fun sendMessageInline(chatId: Long, text: String, keyboard: org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup): org.telegram.telegrambots.meta.api.objects.Message {
        try {
            val message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .replyMarkup(keyboard)
                .build()
            return bot.execute(message)
        } catch (e: Exception) {
            log.error("Failed to send message", e)
            throw e
        }
    }
}

