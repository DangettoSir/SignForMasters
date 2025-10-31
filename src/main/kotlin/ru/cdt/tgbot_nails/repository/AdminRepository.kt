package ru.cdt.tgbot_nails.repository

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Repository

object AdminsTable : Table("admins") {
    val userId = long("user_id")
    override val primaryKey = PrimaryKey(userId)
}

@Repository
class AdminRepository {
    fun isAdmin(userId: Long): Boolean = transaction {
        AdminsTable.selectAll().where { AdminsTable.userId eq userId }.any()
    }

    fun addAdmin(userId: Long): Boolean = transaction {
        AdminsTable.insertIgnore { it[AdminsTable.userId] = userId }.insertedCount > 0
    }

    fun removeAdmin(userId: Long): Boolean = transaction {
        AdminsTable.deleteWhere { AdminsTable.userId eq userId } > 0
    }

    fun listAdmins(): List<Long> = transaction {
        AdminsTable.selectAll().map { it[AdminsTable.userId] }
    }
}




