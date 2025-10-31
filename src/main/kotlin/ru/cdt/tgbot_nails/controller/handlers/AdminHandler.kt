package ru.cdt.tgbot_nails.controller.handlers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import ru.cdt.tgbot_nails.controller.AdminMasterDraft
import ru.cdt.tgbot_nails.controller.UserState
import ru.cdt.tgbot_nails.keyboard.KeyboardFactory
import ru.cdt.tgbot_nails.service.AdminService
import ru.cdt.tgbot_nails.service.AppointmentService
import ru.cdt.tgbot_nails.service.MasterService

/**
 * Обработчик административных команд и управления мастерами.
 * Управляет админ-панелью, добавлением/удалением мастеров, статистикой.
 */
class AdminHandler(
    private val bot: TelegramLongPollingBot,
    private val adminService: AdminService,
    private val masterService: MasterService,
    private val appointmentService: AppointmentService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    /**
     * Генерирует ID мастера из имени (простая замена пробелов на подчеркивание).
     */
    private fun generateMasterId(fullName: String?): String {
        val base = (fullName ?: "master")
            .lowercase()
            .replace(Regex("[^a-zа-яё0-9\\s]"), "")
            .replace(" ", "_")
            .ifEmpty { "master_${System.currentTimeMillis()}" }
            .take(50)
        return base
    }
    
    /**
     * Обрабатывает команду /admin - показывает админ-панель.
     */
    fun handleAdminCommand(chatId: Long, userId: Long, getAdminMessageId: () -> Int?, setAdminMessageId: (Int) -> Unit) {
        try {
            if (adminService.isAdmin(userId)) {
                sendMessageReplyKeyboard(chatId, "🔧 Панель администратора", KeyboardFactory.createAdminMainMenuKeyboard())
                val msgId = getAdminMessageId()
                if (msgId != null) {
                    editMessage(chatId, msgId, "Выберите действие:", KeyboardFactory.createAdminKeyboard())
                } else {
                    val sent = sendMessageInline(chatId, "Выберите действие:", KeyboardFactory.createAdminKeyboard())
                    setAdminMessageId(sent.messageId)
                }
            } else {
                sendMessage(chatId, "У вас нет прав администратора.")
            }
        } catch (e: Exception) {
            sendMessage(chatId, "Произошла ошибка.")
        }
    }
    
    /**
     * Обрабатывает панель администратора.
     */
    fun handleAdminPanel(chatId: Long, getAdminMessageId: () -> Int?, setAdminMessageId: (Int) -> Unit) {
        sendMessageReplyKeyboard(chatId, "🔧 Панель администратора", KeyboardFactory.createAdminMainMenuKeyboard())
        val msgId = getAdminMessageId()
        if (msgId != null) {
            editMessage(chatId, msgId, "Выберите действие:", KeyboardFactory.createAdminKeyboard())
        } else {
            val sent = sendMessageInline(chatId, "Выберите действие:", KeyboardFactory.createAdminKeyboard())
            setAdminMessageId(sent.messageId)
        }
    }
    
    /**
     * Обрабатывает команду /add_admin - добавление администратора.
     */
    fun handleAddAdmin(chatId: Long, text: String) {
        val parts = text.trim().split(" ")
        if (parts.size < 2) {
            sendMessage(chatId, "Использование: /add_admin <userId>")
            return
        }
        val targetId = parts[1].toLongOrNull()
        if (targetId == null) {
            sendMessage(chatId, "userId должен быть числом")
            return
        }
        val ok = adminService.addAdmin(targetId)
        if (ok) sendMessage(chatId, "Админ добавлен: $targetId") else sendMessage(chatId, "Не удалось добавить админа")
    }
    
    /**
     * Обрабатывает команду /remove_admin - удаление администратора.
     */
    fun handleRemoveAdmin(chatId: Long, text: String) {
        val parts = text.trim().split(" ")
        if (parts.size < 2) {
            sendMessage(chatId, "Использование: /remove_admin <userId>")
            return
        }
        val targetId = parts[1].toLongOrNull()
        if (targetId == null) {
            sendMessage(chatId, "userId должен быть числом")
            return
        }
        val ok = adminService.removeAdmin(targetId)
        if (ok) sendMessage(chatId, "Админ удалён: $targetId") else sendMessage(chatId, "Не удалось удалить админа")
    }
    
    /**
     * Показывает статистику записей.
     */
    fun handleAdminStats(chatId: Long, getAdminMessageId: () -> Int?) {
        val appointments = appointmentService.getAllAppointments()
        val totalAppointments = appointments.size
        val confirmedAppointments = appointments.count { it.status == ru.cdt.tgbot_nails.model.AppointmentStatus.CONFIRMED }
        
        val message = """
            📊 Статистика записей
            
            Всего записей: $totalAppointments
            Активных записей: $confirmedAppointments
        """.trimIndent()
        
        val msgId = getAdminMessageId()
        if (msgId != null) {
            editMessage(chatId, msgId, message, KeyboardFactory.createBackKeyboard())
        } else {
            sendMessage(chatId, message, KeyboardFactory.createBackKeyboard())
        }
    }
    
    /**
     * Переход к управлению мастерами.
     */
    fun editToAdminMasters(chatId: Long, messageId: Int) {
        editMessage(chatId, messageId, "Управление мастерами", KeyboardFactory.createMasterManagementKeyboard())
    }
    
    /**
     * Начало добавления мастера - выбор категории.
     */
    fun editToAdminAddMasterCategory(chatId: Long, userId: Long, messageId: Int, setDraft: (AdminMasterDraft) -> Unit, setState: (UserState) -> Unit) {
        if (!adminService.isAdmin(userId)) {
            editMessage(chatId, messageId, "Нет прав", KeyboardFactory.createAdminKeyboard())
            return
        }
        setDraft(AdminMasterDraft())
        setState(UserState.WAITING_MASTER_CATEGORY)
        editMessage(chatId, messageId, "Выберите направление (категорию)", KeyboardFactory.createAdminCategoriesKeyboard())
    }
    
    /**
     * Обрабатывает выбор категории мастера.
     */
    fun handleAdminCategorySelected(chatId: Long, userId: Long, messageId: Int, category: String, getDraft: () -> AdminMasterDraft?, setDraft: (AdminMasterDraft) -> Unit, setState: (UserState) -> Unit) {
        val draft = getDraft() ?: AdminMasterDraft()
        setDraft(draft.copy(category = category))
        setState(UserState.WAITING_MASTER_NAME)
        editMessage(chatId, messageId, "Введите ФИО мастера", KeyboardFactory.createBackKeyboard())
    }
    
    /**
     * Переход к удалению мастера - показывает список.
     */
    fun editToAdminDeleteMaster(chatId: Long, userId: Long, messageId: Int, setState: (UserState) -> Unit) {
        if (!adminService.isAdmin(userId)) {
            editMessage(chatId, messageId, "Нет прав", KeyboardFactory.createAdminKeyboard())
            return
        }
        setState(UserState.WAITING_DELETE_MASTER)
        val masters = masterService.getAllMasters().sortedBy { it.name }
        editMessage(chatId, messageId, "Удалить мастера — выберите из списка", KeyboardFactory.createMastersDeletePage(masters, 0))
    }
    
    /**
     * Обрабатывает навигацию по страницам при удалении мастера.
     */
    fun handleDeleteMastersPage(chatId: Long, userId: Long, messageId: Int, page: Int) {
        if (!adminService.isAdmin(userId)) return
        val masters = masterService.getAllMasters().sortedBy { it.name }
        editMessage(chatId, messageId, "Удалить мастера — выберите из списка", KeyboardFactory.createMastersDeletePage(masters, page))
    }
    
    /**
     * Обрабатывает выбор мастера для удаления (удаление по ID из кнопки).
     */
    fun handleDeleteMasterPick(chatId: Long, userId: Long, messageId: Int, masterId: String) {
        if (!adminService.isAdmin(userId)) return
        val ok = masterService.deleteMaster(masterId)
        val masters = masterService.getAllMasters().sortedBy { it.name }
        val master = masterService.getMasterById(masterId)
        val masterName = master?.name ?: masterId
        val text = if (ok) "✅ Мастер удалён: $masterName" else "❌ Не удалось удалить мастера: $masterName"
        editMessage(chatId, messageId, text, KeyboardFactory.createMastersDeletePage(masters, 0))
    }
    
    /**
     * Обрабатывает ввод имени мастера.
     */
    fun handleAdminMasterName(chatId: Long, userId: Long, text: String, getDraft: () -> AdminMasterDraft?, setDraft: (AdminMasterDraft) -> Unit, setState: (UserState) -> Unit, getAdminMessageId: () -> Int?) {
        val draft = getDraft() ?: return
        setDraft(draft.copy(fullName = text.trim()))
        setState(UserState.WAITING_MASTER_SCHEDULE_DAYS)
        val msgId = getAdminMessageId()
        if (msgId != null) {
            editMessage(chatId, msgId, "Укажи рабочие дни (через запятую):\nнапример: monday,tuesday,friday", KeyboardFactory.createBackKeyboard())
        } else {
            sendMessage(chatId, "Укажи рабочие дни (monday,tuesday,...)", KeyboardFactory.createBackKeyboard())
        }
    }
    
    /**
     * Обрабатывает ввод рабочих дней мастера.
     */
    fun handleAdminMasterDays(chatId: Long, userId: Long, text: String, getDraft: () -> AdminMasterDraft?, setDraft: (AdminMasterDraft) -> Unit, setState: (UserState) -> Unit, getAdminMessageId: () -> Int?) {
        val draft = getDraft() ?: return
        setDraft(draft.copy(workingDays = text.trim().lowercase()))
        setState(UserState.WAITING_MASTER_SCHEDULE_TIME)
        val msgId = getAdminMessageId()
        if (msgId != null) {
            editMessage(chatId, msgId, "Время работы (формат HH:mm-HH:mm). Например: 10:00-18:00", KeyboardFactory.createBackKeyboard())
        } else {
            sendMessage(chatId, "Время работы (HH:mm-HH:mm)", KeyboardFactory.createBackKeyboard())
        }
    }
    
    /**
     * Обрабатывает ввод времени работы мастера.
     */
    fun handleAdminMasterTime(chatId: Long, userId: Long, text: String, getDraft: () -> AdminMasterDraft?, setDraft: (AdminMasterDraft) -> Unit, setState: (UserState) -> Unit, getAdminMessageId: () -> Int?) {
        val draft = getDraft() ?: return
        val parts = text.split("-")
        if (parts.size != 2) {
            val msgId = getAdminMessageId()
            if (msgId != null) editMessage(chatId, msgId, "Неверный формат. Нужно HH:mm-HH:mm", KeyboardFactory.createBackKeyboard())
            else sendMessage(chatId, "Неверный формат. Нужно HH:mm-HH:mm", KeyboardFactory.createBackKeyboard())
            return
        }
        setDraft(draft.copy(timeFrom = parts[0].trim(), timeTo = parts[1].trim()))
        setState(UserState.WAITING_MASTER_SLOT_DURATION)
        val msgId = getAdminMessageId()
        if (msgId != null) {
            editMessage(chatId, msgId, "Средняя длительность сеанса (мин). Например: 30", KeyboardFactory.createBackKeyboard())
        } else {
            sendMessage(chatId, "Средняя длительность сеанса (мин)", KeyboardFactory.createBackKeyboard())
        }
    }
    
    /**
     * Обрабатывает ввод длительности слота и создает мастера.
     */
    fun handleAdminMasterSlot(chatId: Long, userId: Long, text: String, getDraft: () -> AdminMasterDraft?, setDraft: (AdminMasterDraft) -> Unit, setState: (UserState) -> Unit, getAdminMessageId: () -> Int?) {
        val draft = getDraft() ?: return
        val slot = text.trim().toIntOrNull()
        if (slot == null || slot <= 0) {
            val msgId = getAdminMessageId()
            if (msgId != null) editMessage(chatId, msgId, "Нужно число минут > 0", KeyboardFactory.createBackKeyboard())
            else sendMessage(chatId, "Нужно число минут > 0", KeyboardFactory.createBackKeyboard())
            return
        }
        
        val days = draft.workingDays?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val schedule = days.associateWith { 
            ru.cdt.tgbot_nails.model.DaySchedule(
                draft.timeFrom!!, 
                draft.timeTo!!,
                ((java.time.Duration.between(java.time.LocalTime.parse(draft.timeFrom), java.time.LocalTime.parse(draft.timeTo)).toMinutes()) / slot).toInt()
            )
        }
        
        setDraft(draft.copy(slotMinutes = slot))
        setState(UserState.WAITING_MASTER_TELEGRAM_ID)
        val msgId = getAdminMessageId()
        if (msgId != null) {
            editMessage(chatId, msgId, "Введите Telegram ID мастера (число) или отправьте 0 если не нужен:", KeyboardFactory.createBackKeyboard())
        } else {
            sendMessage(chatId, "Введите Telegram ID мастера (число) или отправьте 0 если не нужен:", KeyboardFactory.createBackKeyboard())
        }
    }
    
    /**
     * Обрабатывает ввод Telegram ID мастера и создает мастера.
     */
    fun handleAdminMasterTelegramId(chatId: Long, userId: Long, text: String, getDraft: () -> AdminMasterDraft?, clearDraft: () -> Unit, clearState: () -> Unit, getAdminMessageId: () -> Int?) {
        val draft = getDraft() ?: return
        val telegramId = text.trim().toLongOrNull()
        
        if (telegramId == null) {
            val msgId = getAdminMessageId()
            if (msgId != null) {
                editMessage(chatId, msgId, "Нужно число (Telegram ID) или 0", KeyboardFactory.createBackKeyboard())
            } else {
                sendMessage(chatId, "Нужно число (Telegram ID) или 0", KeyboardFactory.createBackKeyboard())
            }
            return
        }
        
        val days = draft.workingDays?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val schedule = days.associateWith { 
            ru.cdt.tgbot_nails.model.DaySchedule(
                draft.timeFrom!!, 
                draft.timeTo!!,
                ((java.time.Duration.between(java.time.LocalTime.parse(draft.timeFrom), java.time.LocalTime.parse(draft.timeTo)).toMinutes()) / (draft.slotMinutes ?: 30)).toInt()
            )
        }
        
        val id = generateMasterId(draft.fullName)
        val finalTelegramId = if (telegramId == 0L) null else telegramId
        val ok = masterService.addMaster(ru.cdt.tgbot_nails.model.Master(
            id = id,
            name = draft.fullName ?: id,
            specialization = draft.category ?: "",
            schedule = schedule,
            telegramId = finalTelegramId
        ))
        
        clearDraft()
        clearState()
        
        val msgId = getAdminMessageId()
        if (ok) {
            val telegramInfo = if (finalTelegramId != null) "\nTelegram ID: $finalTelegramId" else "\nTelegram ID: не указан"
            if (msgId != null) {
                editMessage(chatId, msgId, "✅ Мастер добавлен: ${draft.fullName}\nКатегория: ${draft.category}\nID: $id$telegramInfo", KeyboardFactory.createBackKeyboard())
            } else {
                sendMessage(chatId, "✅ Мастер добавлен: ${draft.fullName}\nКатегория: ${draft.category}\nID: $id$telegramInfo", KeyboardFactory.createBackKeyboard())
            }
        } else {
            if (msgId != null) {
                editMessage(chatId, msgId, "❌ Не удалось добавить мастера (ID может существовать)", KeyboardFactory.createBackKeyboard())
            } else {
                sendMessage(chatId, "❌ Не удалось добавить мастера (ID может существовать)", KeyboardFactory.createBackKeyboard())
            }
        }
    }
    
    /**
     * Показывает список мастеров для редактирования.
     */
    fun handleAdminEditMaster(chatId: Long, userId: Long) {
        try {
            val masters = masterService.getAllMasters()
            if (masters.isEmpty()) {
                sendMessage(chatId, "Мастера не найдены.")
                return
            }
            
            val message = masters.joinToString("\n\n") { master ->
                """
                    👤 ${master.name}
                    Специализация: ${master.specialization}
                    ID: ${master.id}
                """.trimIndent()
            }
            
            sendMessage(chatId, "Текущие мастера:\n\n$message", KeyboardFactory.createBackKeyboard())
        } catch (e: Exception) {
            sendMessage(chatId, "Произошла ошибка при получении списка мастеров.")
        }
    }
    
    /**
     * Обрабатывает добавление мастера через JSON (старый способ через текстовое сообщение).
     */
    fun handleAdminAddMasterInput(chatId: Long, userId: Long, text: String, clearState: () -> Unit) {
        try {
            val parts = text.split(";", limit = 3)
            if (parts.size < 3) {
                sendMessage(chatId, "Формат: name;specialization;JSON_расписания")
                return
            }
            val name = parts[0].trim()
            val specialization = parts[1].trim()
            val scheduleJson = parts[2].trim()
            val mapper = jacksonObjectMapper()
            val schedule: Map<String, ru.cdt.tgbot_nails.model.DaySchedule> = try {
                mapper.readValue(scheduleJson)
            } catch (e: Exception) {
                sendMessage(chatId, "Некорректный JSON расписания: ${e.message}")
                return
            }
            val masterId = generateMasterId(name)
            val ok = masterService.addMaster(ru.cdt.tgbot_nails.model.Master(
                id = masterId,
                name = name,
                specialization = specialization,
                schedule = schedule
            ))
            if (ok) {
                sendMessage(chatId, "✅ Мастер добавлен:\nID: $masterId\nИмя: $name\nСпец: $specialization")
                clearState()
            } else {
                sendMessage(chatId, "❌ Не удалось добавить мастера (возможно ID уже существует)")
            }
        } catch (e: Exception) {
            sendMessage(chatId, "Ошибка добавления мастера: ${e.message}")
        }
    }
    
    /**
     * Обрабатывает возврат к админ-панели.
     */
    fun handleAdminBack(chatId: Long, getAdminMessageId: () -> Int?) {
        try {
            val msgId = getAdminMessageId()
            if (msgId != null) {
                editMessage(chatId, msgId, "Админ панель", KeyboardFactory.createAdminKeyboard())
            } else {
                sendMessage(chatId, "Админ панель", KeyboardFactory.createAdminKeyboard())
            }
        } catch (e: Exception) {
            sendMessage(chatId, "Произошла ошибка.")
        }
    }
    
    private fun editMessage(chatId: Long, messageId: Int, text: String, keyboard: InlineKeyboardMarkup? = null) {
        try {
            val edit = EditMessageText()
            edit.chatId = chatId.toString()
            edit.messageId = messageId
            edit.text = text
            if (keyboard != null) {
                edit.replyMarkup = keyboard
            }
            bot.execute(edit)
        } catch (e: Exception) {
            log.warn("Failed to edit message: ${e.message}")
            if (keyboard != null) {
                sendMessage(chatId, text, keyboard)
            } else {
                sendMessage(chatId, text)
            }
        }
    }
    
    private fun sendMessage(chatId: Long, text: String, keyboard: InlineKeyboardMarkup? = null) {
        try {
            val builder = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
            if (keyboard != null) {
                builder.replyMarkup(keyboard)
            }
            bot.execute(builder.build())
        } catch (e: Exception) {
            log.error("Failed to send message", e)
        }
    }
    
    private fun sendMessage(chatId: Long, text: String) {
        try {
            val message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build()
            bot.execute(message)
        } catch (e: Exception) {
            log.error("Failed to send message", e)
        }
    }
    
    private fun sendMessageInline(chatId: Long, text: String, keyboard: InlineKeyboardMarkup): org.telegram.telegrambots.meta.api.objects.Message {
        try {
            val message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .replyMarkup(keyboard)
                .build()
            return bot.execute(message)
        } catch (e: Exception) {
            log.error("Failed to send message", e)
            throw e
        }
    }
    
    private fun sendMessageReplyKeyboard(chatId: Long, text: String, keyboard: org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup) {
        try {
            val message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .replyMarkup(keyboard)
                .build()
            bot.execute(message)
        } catch (e: Exception) {
            log.error("Failed to send message", e)
        }
    }
}

