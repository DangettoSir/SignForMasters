package ru.cdt.tgbot_nails.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import ru.cdt.tgbot_nails.model.DaySchedule
import ru.cdt.tgbot_nails.model.Master
import ru.cdt.tgbot_nails.model.MastersConfig
import ru.cdt.tgbot_nails.repository.MasterRepository

/**
 * Сервис мастеров: CRUD через БД, расписание подмешивается из masters.json.
 */
@Service
class MasterService(
    private val masterRepository: MasterRepository
) {
    private val logger = LoggerFactory.getLogger(MasterService::class.java)
    private val objectMapper = jacksonObjectMapper()
    private var idToSchedule: Map<String, Map<String, DaySchedule>> = emptyMap()

    @PostConstruct
    fun loadSchedule() {
        try {
            val resource = ClassPathResource("masters.json")
            val conf = objectMapper.readValue(resource.inputStream, MastersConfig::class.java)
            idToSchedule = conf.masters.associate { it.id to it.schedule }
        } catch (e: Exception) {
            logger.warn("Не удалось загрузить расписание из masters.json: {}", e.message)
            idToSchedule = emptyMap()
        }
    }

    fun getAllMasters(): List<Master> = masterRepository.getAll().map { enrichWithSchedule(it) }

    fun getMasterById(id: String): Master? = masterRepository.getById(id)?.let { enrichWithSchedule(it) }

    fun getMastersBySpecialization(specialization: String): List<Master> =
        masterRepository.getAll().filter { it.specialization.contains(specialization, ignoreCase = true) }.map { enrichWithSchedule(it) }

    fun addMaster(master: Master): Boolean = masterRepository.insert(master)

    fun updateMaster(master: Master): Boolean = masterRepository.update(master)

    fun deleteMaster(masterId: String): Boolean = masterRepository.delete(masterId)

    private fun enrichWithSchedule(master: Master): Master {
        val scheduleFromDb = master.schedule
        if (scheduleFromDb.isNotEmpty()) return master
        val schedule = idToSchedule[master.id] ?: defaultSchedule()
        return master.copy(schedule = schedule)
    }

    private fun defaultSchedule(): Map<String, DaySchedule> = mapOf(
        "monday" to DaySchedule("10:00", "18:00", 16),
        "tuesday" to DaySchedule("10:00", "18:00", 16),
        "wednesday" to DaySchedule("10:00", "18:00", 16),
        "thursday" to DaySchedule("10:00", "18:00", 16),
        "friday" to DaySchedule("10:00", "18:00", 16),
        "saturday" to DaySchedule("11:00", "17:00", 12),
        "sunday" to DaySchedule("11:00", "17:00", 12)
    )
}
