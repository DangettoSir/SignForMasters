package ru.cdt.tgbot_nails.config

import jakarta.annotation.PostConstruct
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * Конфигурация инициализации Exposed.
 * Подключает Exposed к существующему DataSource на старте приложения,
 * чтобы транзакции в репозиториях могли выполняться без ошибок.
 */
@Configuration
class ExposedConfig(
    private val dataSource: DataSource
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Выполняет Database.connect(dataSource) до начала обработки апдейтов Telegram.
     */
    @PostConstruct
    fun initExposed() {
        Database.connect(dataSource)
        log.info("Exposed connected to DataSource successfully")
    }
}


