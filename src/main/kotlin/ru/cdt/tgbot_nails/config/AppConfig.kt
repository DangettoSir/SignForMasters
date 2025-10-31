package ru.cdt.tgbot_nails.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "telegram.bot")
data class TelegramBotConfig(
    var token: String = "",
    var username: String = ""
)

@Component
@ConfigurationProperties(prefix = "app")
data class AppConfig(
    var adminIds: String = "",
    var timezone: String = "Europe/Moscow",
    var slotDurationMinutes: Int = 30
) {
    fun getAdminIdsList(): List<Long> {
        return adminIds.split(",").mapNotNull { it.trim().toLongOrNull() }
    }
}
