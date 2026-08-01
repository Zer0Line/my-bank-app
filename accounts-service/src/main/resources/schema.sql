CREATE SCHEMA IF NOT EXISTS accounts;

CREATE TABLE IF NOT EXISTS accounts.accounts (
    login VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    surename VARCHAR(255) NOT NULL,
    date_of_birth DATE NOT NULL,
    amount numeric NOT NULL
);