package ru.cdt.tgbot_nails.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.generics.TelegramBot
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import ru.cdt.tgbot_nails.controller.NailsBotController

@Configuration
class BotConfig {
    
    @Bean
    fun telegramBotsApi(): TelegramBotsApi {
        return TelegramBotsApi(DefaultBotSession::class.java)
    }
    
    @Bean
    fun registerBot(telegramBotsApi: TelegramBotsApi, bot: NailsBotController): TelegramBot {
        telegramBotsApi.registerBot(bot)
        return bot
    }
}
