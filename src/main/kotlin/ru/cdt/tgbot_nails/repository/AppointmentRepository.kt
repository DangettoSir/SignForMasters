package ru.cdt.tgbot_nails.repository

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import ru.cdt.tgbot_nails.model.Appointment
import ru.cdt.tgbot_nails.model.AppointmentStatus
import java.time.LocalDate
import java.time.LocalTime

object AppointmentsTable : Table("appointments") {
    val id = varchar("id", 200)
    val userId = long("user_id")
    val masterId = varchar("master_id", 64)
    val date = varchar("date", 10) // ISO yyyy-MM-dd
    val time = varchar("time", 8)  // HH:mm:ss
    val status = varchar("status", 32)
    override val primaryKey = PrimaryKey(id)
}

/**
 * Репозиторий записей на Exposed.
 * Реализует CRUD операции для записей на приём.
 */
@Repository
class AppointmentRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    fun insert(appointment: Appointment): Boolean = transaction {
        AppointmentsTable.insertIgnore {
            it[id] = appointment.id
            it[userId] = appointment.userId
            it[masterId] = appointment.masterId
            it[date] = appointment.date.toString()
            it[time] = appointment.time.toString()
            it[status] = appointment.status.name
        }.insertedCount > 0
    }

    fun getById(id: String): Appointment? = transaction {
        AppointmentsTable.selectAll().where { AppointmentsTable.id eq id }.singleOrNull()?.toAppointment()
    }

    fun getByUser(userId: Long): List<Appointment> = transaction {
        AppointmentsTable.selectAll().where { AppointmentsTable.userId eq userId }.map { it.toAppointment() }
    }

    fun getAll(): List<Appointment> = transaction {
        AppointmentsTable.selectAll().map { it.toAppointment() }
    }

    /**
     * Отменяет запись, устанавливая статус CANCELLED.
     */
    fun cancel(id: String): Boolean = transaction {
        log.debug("Cancelling appointment: {}", id)
        val updated = AppointmentsTable.update({ AppointmentsTable.id eq id }) {
            it[status] = AppointmentStatus.CANCELLED.name
        }
        log.debug("Updated {} rows for appointment {}", updated, id)
        updated > 0
    }

    /**
     * Реактивирует отменённую запись (меняет статус на CONFIRMED), если такая запись существует.
     */
    fun reactivateIfCancelled(id: String): Boolean = transaction {
        log.debug("Reactivating cancelled appointment if present: {}", id)
        val updated = AppointmentsTable.update({
            (AppointmentsTable.id eq id) and (AppointmentsTable.status eq AppointmentStatus.CANCELLED.name)
        }) {
            it[status] = AppointmentStatus.CONFIRMED.name
        }
        log.debug("Reactivated rows: {} for appointment {}", updated, id)
        updated > 0
    }

    /**
     * Проверяет наличие активной (не отменённой) записи для мастера на указанную дату и время.
     */
    fun existsForMasterDateTime(masterId: String, date: LocalDate, time: LocalTime): Boolean = transaction {
        log.debug("Checking active appointment for master {} on {} at {}", masterId, date, time)
        val exists = AppointmentsTable.selectAll().where { 
            (AppointmentsTable.masterId eq masterId) and 
            (AppointmentsTable.date eq date.toString()) and 
            (AppointmentsTable.time eq time.toString()) and
            (AppointmentsTable.status eq AppointmentStatus.CONFIRMED.name)
        }.any()
        log.debug("Active appointment exists: {}", exists)
        exists
    }

    private fun ResultRow.toAppointment(): Appointment = Appointment(
        id = this[AppointmentsTable.id],
        userId = this[AppointmentsTable.userId],
        masterId = this[AppointmentsTable.masterId],
        date = java.time.LocalDate.parse(this[AppointmentsTable.date]),
        time = java.time.LocalTime.parse(this[AppointmentsTable.time]),
        status = AppointmentStatus.valueOf(this[AppointmentsTable.status])
    )
}


