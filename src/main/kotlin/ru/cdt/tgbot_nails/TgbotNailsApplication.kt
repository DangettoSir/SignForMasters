package ru.cdt.tgbot_nails

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import ru.cdt.tgbot_nails.config.AppConfig
import ru.cdt.tgbot_nails.config.TelegramBotConfig

@SpringBootApplication
@EnableConfigurationProperties(AppConfig::class, TelegramBotConfig::class)
@EnableScheduling
class TgbotNailsApplication

fun main(args: Array<String>) {
	runApplication<TgbotNailsApplication>(*args)
}
