-- Customer & workforce operational foundation.
-- Apply to user_db before deploying the matching user-service build.
SET NAMES utf8mb4;

-- Keep authentication identity separate from the business persona it is allowed to enter.
ALTER TABLE users
    ADD COLUMN account_type VARCHAR(20) NULL AFTER status;

UPDATE users SET account_type = 'CUSTOMER' WHERE account_type IS NULL;
UPDATE users u
JOIN employees e ON e.account_id = u.account_id AND e.is_deleted = FALSE
SET u.account_type = 'WORKFORCE';

ALTER TABLE users
    MODIFY account_type VARCHAR(20) NOT NULL;

-- A job position is part of the organization model, not a global free-form label.
ALTER TABLE positions
    ADD COLUMN department_id BIGINT NULL AFTER description;

UPDATE positions p
JOIN departments d ON d.code = CASE p.code
    WHEN 'BOX_OFFICE' THEN 'OPS'
    WHEN 'OPS_MANAGER' THEN 'OPS'
    WHEN 'CUSTOMER_CARE' THEN 'CS'
    ELSE NULL
END
SET p.department_id = d.id
WHERE p.department_id IS NULL;

-- Preserve the existing Finance employee without retaining the invalid Finance/Customer Care pair.
INSERT INTO positions (code, title, description, department_id, is_deleted)
SELECT 'FINANCE_ADMIN', 'Finance Administrator',
       CONVERT(0x5175E1BAA36E207472E1BB8B2074C3A069206368C3AD6E68 USING utf8mb4),
       d.id, FALSE
FROM departments d
WHERE d.code = 'FIN'
  AND NOT EXISTS (SELECT 1 FROM positions p WHERE p.code = 'FINANCE_ADMIN');

UPDATE employees e
JOIN departments d ON d.id = e.department_id AND d.code = 'FIN'
JOIN positions old_position ON old_position.id = e.position_id AND old_position.code = 'CUSTOMER_CARE'
JOIN positions new_position ON new_position.code = 'FINANCE_ADMIN'
SET e.position_id = new_position.id;

ALTER TABLE positions
    MODIFY department_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_position_department
        FOREIGN KEY (department_id) REFERENCES departments(id),
    ADD INDEX idx_position_department_active (department_id, is_deleted);

-- Payroll state transitions need ownership, evidence and an optimistic-lock token.
ALTER TABLE payrolls
    ADD COLUMN created_by BIGINT NULL AFTER status,
    ADD COLUMN paid_by BIGINT NULL AFTER approved_at,
    ADD COLUMN payment_reference VARCHAR(100) NULL AFTER paid_by,
    ADD COLUMN cancelled_by BIGINT NULL AFTER paid_at,
    ADD COLUMN cancellation_reason VARCHAR(500) NULL AFTER cancelled_by;

CREATE TABLE IF NOT EXISTS employment_actions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_account_id BIGINT NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    effective_date DATE NOT NULL,
    reason VARCHAR(500) NOT NULL,
    previous_status VARCHAR(20) NULL,
    new_status VARCHAR(20) NULL,
    previous_department_id BIGINT NULL,
    new_department_id BIGINT NULL,
    previous_position_id BIGINT NULL,
    new_position_id BIGINT NULL,
    previous_base_salary DECIMAL(15,2) NULL,
    new_base_salary DECIMAL(15,2) NULL,
    performed_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_employment_actions_employee
        FOREIGN KEY (employee_account_id) REFERENCES employees(account_id),
    CONSTRAINT fk_employment_actions_previous_department
        FOREIGN KEY (previous_department_id) REFERENCES departments(id),
    CONSTRAINT fk_employment_actions_new_department
        FOREIGN KEY (new_department_id) REFERENCES departments(id),
    CONSTRAINT fk_employment_actions_previous_position
        FOREIGN KEY (previous_position_id) REFERENCES positions(id),
    CONSTRAINT fk_employment_actions_new_position
        FOREIGN KEY (new_position_id) REFERENCES positions(id),
    INDEX idx_employment_actions_employee_created (employee_account_id, created_at),
    INDEX idx_employment_actions_effective_date (effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Repair the known seed values that were imported through a non-UTF-8 connection.
UPDATE departments
SET description = CASE code
    WHEN 'OPS' THEN CONVERT(0x52E1BAA1702076C3A02076E1BAAD6E2068C3A06E68 USING utf8mb4)
    WHEN 'CS' THEN CONVERT(0x4368C4836D2073C3B363206B68C3A163682068C3A06E67 USING utf8mb4)
    WHEN 'FIN' THEN CONVERT(0x5468616E6820746FC3A16E2076C3A020C491E1BB916920736FC3A174 USING utf8mb4)
    ELSE description
END
WHERE code IN ('OPS', 'CS', 'FIN');

UPDATE positions
SET description = CASE code
    WHEN 'BOX_OFFICE' THEN CONVERT(0x4E68C3A26E207669C3AA6E207175E1BAA7792076C3A9 USING utf8mb4)
    WHEN 'CUSTOMER_CARE' THEN CONVERT(0x4368C4836D2073C3B363206B68C3A163682068C3A06E67 USING utf8mb4)
    WHEN 'OPS_MANAGER' THEN CONVERT(0x5175E1BAA36E206CC3BD2076E1BAAD6E2068C3A06E68 USING utf8mb4)
    WHEN 'FINANCE_ADMIN' THEN CONVERT(0x5175E1BAA36E207472E1BB8B2074C3A069206368C3AD6E68 USING utf8mb4)
    ELSE description
END
WHERE code IN ('BOX_OFFICE', 'CUSTOMER_CARE', 'OPS_MANAGER', 'FINANCE_ADMIN');
