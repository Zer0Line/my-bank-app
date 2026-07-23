INSERT INTO accounts.accounts (login, name, surename, date_of_birth, amount)
SELECT 'bankuser', 'John', 'Dow', '1986-05-18', 100
WHERE NOT EXISTS (SELECT 1 FROM accounts.accounts WHERE login = 'bankuser');
