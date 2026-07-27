-- User Service keeps a read-only operational email snapshot from ACCOUNT_VERIFIED.
-- Auth Service remains the source of truth for login and account ownership.
ALTER TABLE users
    ADD COLUMN email VARCHAR(100) NULL AFTER account_id,
    ADD INDEX idx_users_email (email);

-- Existing local environments run Auth and User databases on the same MySQL
-- instance. If the database name differs, replace auth_db before running.
UPDATE users user_profile
JOIN auth_db.accounts account ON account.id = user_profile.account_id
SET user_profile.email = LOWER(account.email)
WHERE user_profile.email IS NULL;
