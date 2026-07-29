-- Allow an OAuth-created profile to exist before the customer supplies a phone number.
-- Google OpenID Connect does not include a phone number in the standard profile claims.

ALTER TABLE user_db.users
    MODIFY COLUMN phone_number VARCHAR(15) NULL;

SELECT IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'user_db'
  AND TABLE_NAME = 'users'
  AND COLUMN_NAME = 'phone_number';
