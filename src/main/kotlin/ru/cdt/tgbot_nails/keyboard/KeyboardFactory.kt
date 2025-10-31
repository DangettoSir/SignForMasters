package ru.cdt.tgbot_nails.keyboard

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import ru.cdt.tgbot_nails.model.Master
import ru.cdt.tgbot_nails.model.TimeSlot
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object KeyboardFactory {
    
    fun createMainMenuKeyboard(): ReplyKeyboardMarkup {
        val keyboard = ReplyKeyboardMarkup()
        keyboard.resizeKeyboard = true
        keyboard.oneTimeKeyboard = false
        
        val row1 = KeyboardRow()
        row1.add(KeyboardButton("📅 Записаться на прием"))
        
        val row2 = KeyboardRow()
        row2.add(KeyboardButton("📋 Мои записи"))
        
        val row3 = KeyboardRow()
        row3.add(KeyboardButton("ℹ️ Информация"))
        
        keyboard.keyboard = listOf(row1, row2, row3)
        return keyboard
    }
    
    fun createAdminMainMenuKeyboard(): ReplyKeyboardMarkup {
        val keyboard = ReplyKeyboardMarkup()
        keyboard.resizeKeyboard = true
        keyboard.oneTimeKeyboard = false
        
        val row1 = KeyboardRow()
        row1.add(KeyboardButton("📅 Записаться на прием"))
        
        val row2 = KeyboardRow()
        row2.add(KeyboardButton("📋 Мои записи"))
        
        val row3 = KeyboardRow()
        row3.add(KeyboardButton("ℹ️ Информация"))
        
        val row4 = KeyboardRow()
        row4.add(KeyboardButton("🔧 Админ панель"))
        
        keyboard.keyboard = listOf(row1, row2, row3, row4)
        return keyboard
    }

    fun createInfoCommandsKeyboard(isAdmin: Boolean): ReplyKeyboardMarkup {
        val keyboard = ReplyKeyboardMarkup()
        keyboard.resizeKeyboard = true
        keyboard.oneTimeKeyboard = false

        val row1 = KeyboardRow()
        row1.add(KeyboardButton("/start"))
        row1.add(KeyboardButton("/info"))

        val rows = mutableListOf<KeyboardRow>()
        rows.add(row1)

        if (isAdmin) {
            val row2 = KeyboardRow()
            row2.add(KeyboardButton("/admin"))
            rows.add(row2)

            val row3 = KeyboardRow()
            row3.add(KeyboardButton("/add_admin"))
            row3.add(KeyboardButton("/remove_admin"))
            rows.add(row3)
        }

        keyboard.keyboard = rows
        return keyboard
    }
    
    fun createPhoneRequestKeyboard(): ReplyKeyboardMarkup {
        val keyboard = ReplyKeyboardMarkup()
        keyboard.resizeKeyboard = true
        keyboard.oneTimeKeyboard = true
        
        val row = KeyboardRow()
        val phoneButton = KeyboardButton("📱 Поделиться номером телефона")
        phoneButton.requestContact = true
        row.add(phoneButton)
        
        keyboard.keyboard = listOf(row)
        return keyboard
    }
    
    fun createMastersKeyboard(masters: List<Master>): InlineKeyboardMarkup {
        val keyboard = InlineKeyboardMarkup()
        val buttons = masters.map { master ->
            listOf(InlineKeyboardButton.builder()
                .text("${master.name} - ${master.specialization}")
                .callbackData("master_${master.id}")
                .build())
        }
        keyboard.keyboard = buttons
        return keyboard
    }
    
    fun createDateKeyboard(): InlineKeyboardMarkup {
        val keyboard = InlineKeyboardMarkup()
        val today = LocalDate.now()
        val buttons = mutableListOf<List<InlineKeyboardButton>>()
        
        for (i in 0..13) {
            val date = today.plusDays(i.toLong())
            val formatter = DateTimeFormatter.ofPattern("dd.MM (E)")
            val buttonText = if (i == 0) "Сегодня" else if (i == 1) "Завтра" else date.format(formatter)
            
            buttons.add(listOf(InlineKeyboardButton.builder()
                .text(buttonText)
                .callbackData("date_${date}")
                .build()))
        }
        
        keyboard.keyboard = buttons
        return keyboard
    }

    fun createDatesKeyboard(dates: List<LocalDate>, weekOffset: Int = 0): InlineKeyboardMarkup {
        val keyboard = InlineKeyboardMarkup()
        val buttons = mutableListOf<List<InlineKeyboardButton>>()
        
        // Показываем только 7 дней текущей недели
        val startIdx = weekOffset * 7
        val endIdx = (startIdx + 7).coerceAtMost(dates.size)
        val weekDates = dates.subList(startIdx, endIdx)
        
        weekDates.forEachIndexed { idx, date ->
            val formatter = DateTimeFormatter.ofPattern("dd.MM (E)")
            val buttonText = when (idx) {
                0 -> "Сегодня"
                1 -> "Завтра"
                else -> date.format(formatter)
            }
            buttons.add(listOf(InlineKeyboardButton.builder()
                .text(buttonText)
                .callbackData("date_${date}")
                .build()))
        }
        
        // Навигация по неделям
        val navRow = mutableListOf<InlineKeyboardButton>()
        if (weekOffset > 0) {
            navRow.add(InlineKeyboardButton.builder()
                .text("⬅️ Неделя ${weekOffset}")
                .callbackData("week_${weekOffset - 1}")
                .build())
        }
        navRow.add(InlineKeyboardButton.builder()
            .text("Неделя ${weekOffset + 1}")
            .callbackData("noop")
            .build())
        if (endIdx < dates.size) {
            navRow.add(InlineKeyboardButton.builder()
                .text("Неделя ${weekOffset + 2} ➡️")
                .callbackData("week_${weekOffset + 1}")
                .build())
        }
        if (navRow.isNotEmpty()) {
            buttons.add(navRow)
        }
        
        // Кнопка Назад
        buttons.add(listOf(
            InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("back")
                .build()
        ))
        
        keyboard.keyboard = buttons
        return keyboard
    }
    
    fun createTimeSlotsKeyboard(timeSlots: List<TimeSlot>): InlineKeyboardMarkup {
        val keyboard = InlineKeyboardMarkup()
        val buttons = mutableListOf<List<InlineKeyboardButton>>()
        
        // Фильтруем только доступные слоты
        val availableSlots = timeSlots.filter { it.isAvailable }
        
        if (availableSlots.isEmpty()) {
            // Если нет доступных слотов, показываем сообщение
            val row = listOf(
                InlineKeyboardButton.builder()
                    .text("❌ Нет доступных слотов")
                    .callbackData("no_slots")
                    .build()
            )
            buttons.add(row)
        } else {
            // Показываем только доступные слоты
            availableSlots.chunked(3).forEach { chunk ->
                val row = chunk.map { slot ->
                    val timeText = slot.time.format(DateTimeFormatter.ofPattern("HH:mm"))
                    
                    InlineKeyboardButton.builder()
                        .text(timeText)
                        .callbackData("time_${slot.time}")
                        .build()
                }
                buttons.add(row)
            }
        }
        
        // Добавляем кнопку Назад
        buttons.add(listOf(
            InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("back")
                .build()
        ))
        
        keyboard.keyboard = buttons
        return keyboard
    }
    
    fun createConfirmationKeyboard(): InlineKeyboardMarkup {
        val keyboard = InlineKeyboardMarkup()
        val buttons = listOf(
            listOf(
                InlineKeyboardButton.builder()
                    .text("✅ Подтвердить")
                    .callbackData("confirm_booking")
                    .build(),
                InlineKeyboardButton.builder()
                    .text("❌ Отменить")
                    .callbackData("cancel_booking")
                    .build()
            )
        )
        keyboard.keyboard = buttons
        return keyboard
    }
    
    fun createAdminKeyboard(): InlineKeyboardMarkup {
        val keyboard = InlineKeyboardMarkup()
        val buttons = listOf(
            listOf(InlineKeyboardButton.builder()
                .text("👥 Управление мастерами")
                .callbackData("admin_masters")
                .build()),
            listOf(InlineKeyboardButton.builder()
                .text("📊 Статистика записей")
                .callbackData("admin_stats")
                .build()),
            listOf(InlineKeyboardButton.builder()
                .text("🔙 Назад в меню")
                .callbackData("back_to_main")
                .build())
        )
        keyboard.keyboard = buttons
        return keyboard
    }

    fun createAdminCategoriesKeyboard(): InlineKeyboardMarkup {
        val keyboard = InlineKeyboardMarkup()
        val buttons = listOf(
            listOf(InlineKeyboardButton.builder().text("Маникюр").callbackData("cat_Маникюр").build()),
            listOf(InlineKeyboardButton.builder().text("Педикюр").callbackData("cat_Педикюр").build()),
            listOf(InlineKeyboardButton.builder().text("🔙 Назад").callbackData("admin_back").build())
        )
        keyboard.keyboard = buttons
        return keyboard
    }
    
    fun createMasterManagementKeyboard(): InlineKeyboardMarkup {
        val keyboard = InlineKeyboardMarkup()
        val buttons = listOf(
            listOf(InlineKeyboardButton.builder()
                .text("➕ Добавить мастера")
                .callbackData("admin_add_master")
                .build()),
            listOf(InlineKeyboardButton.builder()
                .text("✏️ Редактировать мастера")
                .callbackData("admin_edit_master")
                .build()),
            listOf(InlineKeyboardButton.builder()
                .text("🗑️ Удалить мастера")
                .callbackData("admin_delete_master")
                .build()),
            listOf(InlineKeyboardButton.builder()
                .text("🔙 Назад")
                .callbackData("admin_back")
                .build())
        )
        keyboard.keyboard = buttons
        return keyboard
    }
    
    fun createAppointmentsKeyboard(appointments: List<ru.cdt.tgbot_nails.model.Appointment>, masterService: ru.cdt.tgbot_nails.service.MasterService): InlineKeyboardMarkup {
        val keyboard = InlineKeyboardMarkup()
        val buttons = mutableListOf<List<InlineKeyboardButton>>()
        
        appointments.forEach { appointment ->
            val master = masterService.getMasterById(appointment.masterId)
            val buttonText = "${master?.name ?: "Мастер"} (${master?.specialization ?: ""}) - ${appointment.date.format(DateTimeFormatter.ofPattern("dd.MM"))} ${appointment.time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            
            buttons.add(listOf(InlineKeyboardButton.builder()
                .text(buttonText)
                .callbackData("cancel_appointment_${appointment.id}")
                .build()))
        }
        
        if (buttons.isNotEmpty()) {
            buttons.add(listOf(InlineKeyboardButton.builder()
                .text("🔙 Назад")
                .callbackData("back")
                .build()))
        }
        
        keyboard.keyboard = buttons
        return keyboard
    }
    
    fun createBackKeyboard(): InlineKeyboardMarkup {
        val keyboard = InlineKeyboardMarkup()
        val buttons = listOf(
            listOf(InlineKeyboardButton.builder()
                .text("🔙 Назад")
                .callbackData("back")
                .build())
        )
        keyboard.keyboard = buttons
        return keyboard
    }

    fun createMastersDeletePage(masters: List<Master>, page: Int, pageSize: Int = 8): InlineKeyboardMarkup {
        val keyboard = InlineKeyboardMarkup()
        val buttons = mutableListOf<List<InlineKeyboardButton>>()
        val totalPages = if (masters.isEmpty()) 1 else ((masters.size - 1) / pageSize) + 1
        val p = page.coerceIn(0, totalPages - 1)
        val from = p * pageSize
        val to = (from + pageSize).coerceAtMost(masters.size)
        masters.subList(from, to).forEachIndexed { idx, m ->
            val num = from + idx + 1
            buttons.add(listOf(InlineKeyboardButton.builder()
                .text("${num}. ${m.name} (${m.id})")
                .callbackData("del_pick_${m.id}")
                .build()))
        }
        val navRow = mutableListOf<InlineKeyboardButton>()
        navRow.add(InlineKeyboardButton.builder().text("⬅️").callbackData("del_page_${(p-1).coerceAtLeast(0)}").build())
        navRow.add(InlineKeyboardButton.builder().text("${p+1}/${totalPages}").callbackData("noop").build())
        navRow.add(InlineKeyboardButton.builder().text("➡️").callbackData("del_page_${(p+1).coerceAtMost(totalPages-1)}").build())
        buttons.add(navRow)
        buttons.add(listOf(InlineKeyboardButton.builder().text("🔙 Назад").callbackData("admin_back").build()))
        keyboard.keyboard = buttons
        return keyboard
    }
}
