package ru.cdt.tgbot_nails.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.cdt.tgbot_nails.model.Appointment
import java.time.format.DateTimeFormatter

/**
 * Сервис уведомлений для мастеров.
 * Отправляет уведомления о предстоящих сеансах и пропущенных записях.
 */
@Service
class MasterNotificationService(
    private val bot: TelegramLongPollingBot
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    /**
     * Отправляет уведомление мастеру о предстоящем сеансе.
     */
    fun sendUpcomingAppointmentNotification(
        masterTelegramId: Long,
        appointment: Appointment,
        timeText: String
    ) {
        try {
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            
            val message = """
                🔔 Уведомление мастера
                
                У вас сеанс через $timeText:
                
                Клиент ID: ${appointment.userId}
                Дата и время: ${appointment.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} ${appointment.time.format(DateTimeFormatter.ofPattern("HH:mm"))}
                
                Подготовьтесь к приему клиента!
            """.trimIndent()
            
            sendMessage(masterTelegramId, message)
            log.info("Sent upcoming appointment notification to master {}", masterTelegramId)
            
        } catch (e: Exception) {
            log.error("Error sending upcoming appointment notification to master {}", masterTelegramId, e)
        }
    }
    
    /**
     * Отправляет уведомление мастеру о пропущенном сеансе.
     */
    fun sendMissedAppointmentNotification(
        masterTelegramId: Long,
        appointment: Appointment
    ) {
        try {
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            
            val message = """
                ❌ Пропущенный сеанс
                
                Клиент не пришел на сеанс:
                
                Клиент ID: ${appointment.userId}
                Дата и время: ${appointment.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} ${appointment.time.format(DateTimeFormatter.ofPattern("HH:mm"))}
                
                Сеанс автоматически отменен.
            """.trimIndent()
            
            sendMessage(masterTelegramId, message)
            log.info("Sent missed appointment notification to master {}", masterTelegramId)
            
        } catch (e: Exception) {
            log.error("Error sending missed appointment notification to master {}", masterTelegramId, e)
        }
    }
    
    /**
     * Отправляет сообщение мастеру.
     */
    private fun sendMessage(chatId: Long, text: String) {
        try {
            val message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build()
            
            bot.execute(message)
        } catch (e: Exception) {
            log.error("Failed to send message to master {}", chatId, e)
        }
    }
}
