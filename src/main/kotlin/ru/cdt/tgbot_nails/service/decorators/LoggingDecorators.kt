package ru.cdt.tgbot_nails.service.decorators

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.cdt.tgbot_nails.model.Master
import ru.cdt.tgbot_nails.model.User
import ru.cdt.tgbot_nails.service.AppointmentService
import ru.cdt.tgbot_nails.service.MasterService

/**
 * Декоратор логирования для MasterService.
 */
@Component
class MasterServiceLoggingDecorator(private val delegate: MasterService) {
    private val logger = LoggerFactory.getLogger(MasterServiceLoggingDecorator::class.java)

    fun getAllMastersLogged() = runCatching {
        logger.info("getAllMasters")
        delegate.getAllMasters()
    }.getOrElse { e ->
        logger.error("getAllMasters failed", e)
        emptyList()
    }

    fun addMasterLogged(master: Master): Boolean {
        logger.info("addMaster id={}", master.id)
        return delegate.addMaster(master)
    }

    fun deleteMasterLogged(id: String): Boolean {
        logger.info("deleteMaster id={}", id)
        return delegate.deleteMaster(id)
    }
}

/**
 * Декоратор логирования для AppointmentService (пример точечных вызовов).
 */
@Component
class AppointmentServiceLoggingDecorator(private val delegate: AppointmentService) {
    private val logger = LoggerFactory.getLogger(AppointmentServiceLoggingDecorator::class.java)

    fun registerUserLogged(user: User) = runCatching {
        logger.info("registerUser id={}", user.id)
        delegate.registerUser(user)
    }.getOrNull()
}


