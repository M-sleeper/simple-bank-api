CREATE TABLE account (
       id SERIAL NOT NULL,
       full_name VARCHAR(100),
       balance NUMERIC DEFAULT 0
       constraint balance_non_negative check (balance >= 0)
);
