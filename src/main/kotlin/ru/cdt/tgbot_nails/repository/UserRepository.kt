package ru.cdt.tgbot_nails.repository

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import ru.cdt.tgbot_nails.model.User

object UsersTable : Table("users") {
    val id = long("id")
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100).nullable()
    val username = varchar("username", 100).nullable()
    val phoneNumber = varchar("phone_number", 32).nullable()
    val isRegistered = bool("is_registered")
    override val primaryKey = PrimaryKey(id)
}

/**
 * Репозиторий пользователей на Exposed.
 * Реализует операции создания, чтения, обновления пользователей в БД.
 */
@Repository
class UserRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    fun upsert(user: User): User = transaction {
        val exists = UsersTable.selectAll().where { UsersTable.id eq user.id }.limit(1).any()
        if (exists) {
            UsersTable.update({ UsersTable.id eq user.id }) {
                it[firstName] = user.firstName
                it[lastName] = user.lastName
                it[username] = user.username
                it[phoneNumber] = user.phoneNumber
                it[isRegistered] = user.isRegistered
            }
        } else {
            UsersTable.insert {
                it[id] = user.id
                it[firstName] = user.firstName
                it[lastName] = user.lastName
                it[username] = user.username
                it[phoneNumber] = user.phoneNumber
                it[isRegistered] = user.isRegistered
            }
        }
        getById(user.id)!!
    }

    fun updatePhone(userId: Long, phone: String): User? = transaction {
        UsersTable.update({ UsersTable.id eq userId }) {
            it[phoneNumber] = phone
        }
        getById(userId)
    }

    fun getById(userId: Long): User? = transaction {
        UsersTable.selectAll().where { UsersTable.id eq userId }.singleOrNull()?.toUser()
    }

    private fun ResultRow.toUser(): User = User(
        id = this[UsersTable.id],
        firstName = this[UsersTable.firstName],
        lastName = this[UsersTable.lastName],
        username = this[UsersTable.username],
        phoneNumber = this[UsersTable.phoneNumber],
        isRegistered = this[UsersTable.isRegistered]
    )
}


