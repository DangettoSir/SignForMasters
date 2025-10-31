-- Инициализация схемы БД: пользователи, мастера, записи, администраторы

-- Таблица пользователей
create table if not exists users (
    id bigint primary key,
    first_name varchar(100) not null,
    last_name varchar(100),
    username varchar(100),
    phone_number varchar(32),
    is_registered boolean not null default false,
    created_at timestamp not null default now()
);

-- Таблица мастеров (с полем для расписания в JSON)
create table if not exists masters (
    id varchar(64) primary key,
    name varchar(120) not null,
    specialization varchar(200) not null,
    schedule_json text,
    created_at timestamp not null default now()
);

-- Таблица записей (дата и время хранятся как varchar для совместимости)
create table if not exists appointments (
    id varchar(200) primary key,
    user_id bigint not null references users(id) on delete cascade,
    master_id varchar(64) not null references masters(id) on delete cascade,
    date varchar(10) not null,
    time varchar(8) not null,
    status varchar(32) not null,
    created_at timestamp not null default now(),
    unique (master_id, date, time)
);

-- Таблица администраторов
create table if not exists admins (
    user_id bigint primary key
);

-- Индексы для оптимизации запросов
create index if not exists idx_appointments_user on appointments(user_id);
create index if not exists idx_appointments_master_date on appointments(master_id, date);
