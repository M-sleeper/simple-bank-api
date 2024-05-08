create table account (
       id serial not null,
       full_name varchar(100),
       balance numeric default 0,
       primary key(id),
       constraint balance_non_negative check (balance >= 0)
);
