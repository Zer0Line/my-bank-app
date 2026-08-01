INSERT INTO accounts.accounts (login, name, surename, date_of_birth, amount)
SELECT 'bankuser', 'John', 'Dow', '1986-05-18', 100
WHERE NOT EXISTS (SELECT 1 FROM accounts.accounts WHERE login = 'bankuser');

INSERT INTO accounts.accounts (login, name, surename, date_of_birth, amount)
SELECT 'alice', 'Alice', 'Smith', '1990-03-12', 250
WHERE NOT EXISTS (SELECT 1 FROM accounts.accounts WHERE login = 'alice');

INSERT INTO accounts.accounts (login, name, surename, date_of_birth, amount)
SELECT 'bob', 'Bob', 'Johnson', '1978-11-25', 500
WHERE NOT EXISTS (SELECT 1 FROM accounts.accounts WHERE login = 'bob');
