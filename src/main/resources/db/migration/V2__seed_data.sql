-- Начальные данные: мастера и администраторы

-- Начальные мастера
insert into masters(id, name, specialization)
values
    ('m1', 'Алина', 'Маникюр'),
    ('m2', 'Кристина', 'Педикюр')
on conflict (id) do nothing;

-- Начальный администратор
insert into admins(user_id)
values (1108777017)
on conflict (user_id) do nothing;

