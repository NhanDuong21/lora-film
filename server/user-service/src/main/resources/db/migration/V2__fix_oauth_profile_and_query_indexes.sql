-- OAuth providers do not guarantee a phone claim. A customer may complete it later.
ALTER TABLE users
    MODIFY COLUMN phone_number VARCHAR(15) NULL;

CREATE INDEX idx_users_status_deleted
    ON users (status, is_deleted);

CREATE INDEX idx_employees_filters
    ON employees (is_deleted, status, department_id, position_id);

CREATE INDEX idx_payrolls_status_month
    ON payrolls (status, salary_month);

CREATE INDEX idx_customer_profiles_joined
    ON customer_profiles (joined_at);
