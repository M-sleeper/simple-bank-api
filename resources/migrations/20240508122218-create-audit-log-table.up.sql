CREATE TABLE audit_log (
       id SERIAL NOT NULL,
       amount NUMERIC,
       account_from int,
       account_to int,
       constraint fk_account_from
                  foreign key(account_from)
                          references account(id),
       constraint fk_account_to
                  foreign key(account_to)
                          references account(id)
);
