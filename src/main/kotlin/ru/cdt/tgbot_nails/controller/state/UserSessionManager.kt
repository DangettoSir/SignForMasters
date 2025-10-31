package ru.cdt.tgbot_nails.controller.state

import ru.cdt.tgbot_nails.controller.AdminMasterDraft
import ru.cdt.tgbot_nails.controller.BookingData
import ru.cdt.tgbot_nails.controller.UserState

/**
 * Менеджер состояний пользователей и их сессионных данных.
 * Управляет жизненным циклом состояний, данных бронирования и черновиков админ-форм.
 */
class UserSessionManager {
    private val userStates = mutableMapOf<Long, UserState>()
    private val bookingData = mutableMapOf<Long, BookingData>()
    private val adminDrafts = mutableMapOf<Long, AdminMasterDraft>()
    private val adminMessageIdByUser = mutableMapOf<Long, Int>()
    private val userWeekOffsets = mutableMapOf<Long, Int>()
    private val bookingMessageIdByUser = mutableMapOf<Long, Int>()
    
    fun getState(userId: Long): UserState? = userStates[userId]
    
    fun setState(userId: Long, state: UserState?) {
        if (state == null) {
            userStates.remove(userId)
        } else {
            userStates[userId] = state
        }
    }
    
    fun getBookingData(userId: Long): BookingData? = bookingData[userId]
    
    fun setBookingData(userId: Long, data: BookingData?) {
        if (data == null) {
            bookingData.remove(userId)
        } else {
            bookingData[userId] = data
        }
    }
    
    fun getAdminDraft(userId: Long): AdminMasterDraft? = adminDrafts[userId]
    
    fun setAdminDraft(userId: Long, draft: AdminMasterDraft?) {
        if (draft == null) {
            adminDrafts.remove(userId)
        } else {
            adminDrafts[userId] = draft
        }
    }
    
    fun getAdminMessageId(userId: Long): Int? = adminMessageIdByUser[userId]
    
    fun setAdminMessageId(userId: Long, messageId: Int?) {
        if (messageId == null) {
            adminMessageIdByUser.remove(userId)
        } else {
            adminMessageIdByUser[userId] = messageId
        }
    }
    
    fun getWeekOffset(userId: Long): Int = userWeekOffsets[userId] ?: 0
    
    fun setWeekOffset(userId: Long, offset: Int) {
        userWeekOffsets[userId] = offset
    }
    
    fun getBookingMessageId(userId: Long): Int? = bookingMessageIdByUser[userId]
    
    fun setBookingMessageId(userId: Long, messageId: Int?) {
        if (messageId == null) {
            bookingMessageIdByUser.remove(userId)
        } else {
            bookingMessageIdByUser[userId] = messageId
        }
    }
    
    fun clearUserSession(userId: Long) {
        userStates.remove(userId)
        bookingData.remove(userId)
        adminDrafts.remove(userId)
        adminMessageIdByUser.remove(userId)
        userWeekOffsets.remove(userId)
        bookingMessageIdByUser.remove(userId)
    }
}

