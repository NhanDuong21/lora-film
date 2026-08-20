-- Optional local/UAT fixture for the Birthday Reward playbook.
-- Run only in demo environments. It deliberately gives three existing,
-- active CUSTOMER accounts a birthday matching the database server's date.
-- The years remain different so age-related reporting still has realistic data.

UPDATE user_db.users
SET birthday = CASE account_id
        WHEN 6 THEN DATE_SUB(CURDATE(), INTERVAL 24 YEAR)
        WHEN 9 THEN DATE_SUB(CURDATE(), INTERVAL 27 YEAR)
        WHEN 13 THEN DATE_SUB(CURDATE(), INTERVAL 31 YEAR)
    END,
    birth_year = CASE account_id
        WHEN 6 THEN YEAR(DATE_SUB(CURDATE(), INTERVAL 24 YEAR))
        WHEN 9 THEN YEAR(DATE_SUB(CURDATE(), INTERVAL 27 YEAR))
        WHEN 13 THEN YEAR(DATE_SUB(CURDATE(), INTERVAL 31 YEAR))
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE account_id IN (6, 9, 13)
  AND account_type = 'CUSTOMER'
  AND status = 'ACTIVE'
  AND is_deleted = 0;
