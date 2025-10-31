package ru.cdt.tgbot_nails.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class User(
    val id: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?,
    val phoneNumber: String? = null,
    val isRegistered: Boolean = false
)

data class Master(
    val id: String,
    val name: String,
    val specialization: String,
    val schedule: Map<String, DaySchedule>,
    val telegramId: Long? = null
)

data class DaySchedule(
    val start: String,
    val end: String,
    val slots: Int
)

data class TimeSlot(
    val masterId: String,
    val date: LocalDate,
    val time: LocalTime,
    val isAvailable: Boolean = true,
    val userId: Long? = null
)

data class Appointment(
    val id: String,
    val userId: Long,
    val masterId: String,
    val date: LocalDate,
    val time: LocalTime,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val status: AppointmentStatus = AppointmentStatus.CONFIRMED
)

enum class AppointmentStatus {
    CONFIRMED,
    COMPLETED,
    CANCELLED
}

data class MastersConfig(
    val masters: List<Master>
)
