package ru.cdt.tgbot_nails.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * Конфигурация DataSource/HikariCP и инициализация Exposed.
 */
@Configuration
class DbConfig {

    @Bean
    fun dataSource(
        @Value("\${spring.datasource.url}") url: String,
        @Value("\${spring.datasource.username}") username: String,
        @Value("\${spring.datasource.password}") password: String,
        @Value("\${spring.datasource.hikari.maximum-pool-size:10}") maxPool: Int,
        @Value("\${spring.datasource.hikari.minimum-idle:2}") minIdle: Int,
        @Value("\${spring.datasource.hikari.pool-name:tg-nails-pool}") poolName: String,
    ): DataSource {
        val cfg = HikariConfig().apply {
            jdbcUrl = url
            this.username = username
            this.password = password
            maximumPoolSize = maxPool
            minimumIdle = minIdle
            this.poolName = poolName
        }
        return HikariDataSource(cfg)
    }

    @Bean
    fun exposedDatabase(dataSource: DataSource): Database = Database.connect(dataSource)
}


