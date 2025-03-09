--liquibase formatted sql

--changeset rafael:create-initial-tables runAlways:false
create table users
(
    username varchar(32) primary key,
    password varchar(128) not null,
    enabled  boolean      not null
);

create table authorities
(
    username  varchar(32) not null,
    authority varchar(50) not null,
    constraint fk_authorities_users foreign key (username) references users (username)
);
create unique index ix_auth_username on authorities (username, authority);

create table if not exists orders
(
    id               uuid primary key,
    currency_to_sell varchar(8)     not null,
    currency_to_buy  varchar(8)     not null,
    amount           numeric(19, 2) not null,
    rate             numeric(19, 6) not null,
    active           boolean        not null
);
CREATE INDEX idx_orders_currency_sell_buy_rate_asc
    ON orders (currency_to_sell, currency_to_buy, rate ASC);
CREATE INDEX idx_orders_currency_sell_buy_rate_desc
    ON orders (currency_to_sell, currency_to_buy, rate DESC);

create table if not exists user_datas
(
    id       uuid primary key,
    username varchar(32) not null,
    email    varchar(50) not null,
    telegram varchar(50) not null,
    constraint fk_user_data_users foreign key (username) references users (username)
);
create unique index ix_user_data_username on user_datas (username);

create table if not exists actives
(
    id             uuid primary key,
    username       varchar(32)    not null,
    currency       varchar(8)     not null,
    amount         numeric(19, 2) not null default 0.00,
    blocked_amount numeric(19, 2) not null default 0.00,
    constraint fk_actives_users foreign key (username) references users (username)
);
create index ix_actives_username on actives (username);
