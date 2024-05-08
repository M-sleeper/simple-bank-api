create table audit_log (
       id serial not null,
       amount numeric not null,
       account_from int default null,
       account_to int default null,
       date_created timestamp DEFAULT now() NOT NULL,
       constraint fk_account_from
                  foreign key(account_from)
                          references account(id),
       constraint fk_account_to
                  foreign key(account_to)
                          references account(id)
);
