package ru.cdt.tgbot_nails.repository

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Repository
import ru.cdt.tgbot_nails.model.DaySchedule
import ru.cdt.tgbot_nails.model.Master

object MastersTable : Table("masters") {
    val id = varchar("id", 64)
    val name = varchar("name", 120)
    val specialization = varchar("specialization", 200)
    val scheduleJson = text("schedule_json").nullable()
    override val primaryKey = PrimaryKey(id)
}

/**
 * Репозиторий мастеров на Exposed (хранит расписание в JSON).
 */
@Repository
class MasterRepository {
    private val mapper = jacksonObjectMapper()

    fun getAll(): List<Master> = transaction {
        MastersTable.selectAll().map { it.toMaster(mapper) }
    }

    fun getById(masterId: String): Master? = transaction {
        MastersTable.selectAll().where { MastersTable.id eq masterId }.singleOrNull()?.toMaster(mapper)
    }

    fun insert(master: Master): Boolean = transaction {
        MastersTable.insertIgnore {
            it[id] = master.id
            it[name] = master.name
            it[specialization] = master.specialization
            it[scheduleJson] = serializeSchedule(master.schedule)
        }.insertedCount > 0
    }

    fun update(master: Master): Boolean = transaction {
        MastersTable.update({ MastersTable.id eq master.id }) {
            it[name] = master.name
            it[specialization] = master.specialization
            it[scheduleJson] = serializeSchedule(master.schedule)
        } > 0
    }

    fun delete(masterId: String): Boolean = transaction {
        MastersTable.deleteWhere { id eq masterId } > 0
    }

    private fun serializeSchedule(schedule: Map<String, DaySchedule>): String =
        mapper.writeValueAsString(schedule)

    private fun ResultRow.toMaster(mapper: com.fasterxml.jackson.databind.ObjectMapper): Master {
        val schedule: Map<String, DaySchedule> = this[MastersTable.scheduleJson]?.let {
            mapper.readValue(it, mapper.typeFactory.constructMapType(Map::class.java, String::class.java, DaySchedule::class.java))
        } ?: emptyMap()
        return Master(
            id = this[MastersTable.id],
            name = this[MastersTable.name],
            specialization = this[MastersTable.specialization],
            schedule = schedule
        )
    }
}


