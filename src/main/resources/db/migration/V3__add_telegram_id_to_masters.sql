-- Добавляем поле telegram_id в таблицу masters
ALTER TABLE masters ADD COLUMN telegram_id BIGINT;

-- Добавляем комментарий к полю
COMMENT ON COLUMN masters.telegram_id IS 'Telegram ID мастера для уведомлений';
