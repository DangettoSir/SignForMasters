package ru.cdt.tgbot_nails.service

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.cdt.tgbot_nails.config.AppConfig
import ru.cdt.tgbot_nails.repository.AdminRepository

/**
 * Сервис управления администраторами.
 * Предоставляет функции проверки прав, добавления и удаления администраторов.
 */
@Service
class AdminService(
    private val appConfig: AppConfig,
    private val adminRepository: AdminRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    /**
     * Инициализирует начальных администраторов из конфигурации при старте приложения.
     */
    @PostConstruct
    fun seedAdmins() {
        log.info("Seeding initial admins from config")
        val adminIds = appConfig.getAdminIdsList()
        adminIds.forEach { 
            adminRepository.addAdmin(it)
            log.info("Added admin: {}", it)
        }
    }

    /**
     * Проверяет, является ли пользователь администратором.
     */
    fun isAdmin(userId: Long): Boolean {
        val result = adminRepository.isAdmin(userId)
        log.debug("Admin check for user {}: {}", userId, result)
        return result
    }

    /**
     * Получает список всех администраторов.
     */
    fun getAllAdminIds(): List<Long> = adminRepository.listAdmins()

    /**
     * Добавляет нового администратора.
     */
    fun addAdmin(userId: Long): Boolean {
        log.info("Adding admin: {}", userId)
        return adminRepository.addAdmin(userId)
    }

    /**
     * Удаляет администратора.
     */
    fun removeAdmin(userId: Long): Boolean {
        log.info("Removing admin: {}", userId)
        return adminRepository.removeAdmin(userId)
    }
}
