CREATE TABLE account (
       id SERIAL NOT NULL,
       full_name VARCHAR(100),
       balance NUMERIC DEFAULT 0
);
