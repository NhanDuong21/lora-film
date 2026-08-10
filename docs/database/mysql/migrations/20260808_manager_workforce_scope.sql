-- Liên kết hồ sơ nhân viên với rạp làm việc để Manager chỉ quản lý đúng phạm vi.
SET @employee_cinema_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = 'user_db'
      AND table_name = 'employees'
      AND column_name = 'cinema_public_id'
);
SET @employee_cinema_column_sql = IF(
    @employee_cinema_column_exists = 0,
    'ALTER TABLE user_db.employees ADD COLUMN cinema_public_id VARCHAR(36) NULL AFTER hire_date',
    'SELECT 1'
);
PREPARE employee_cinema_column_statement FROM @employee_cinema_column_sql;
EXECUTE employee_cinema_column_statement;
DEALLOCATE PREPARE employee_cinema_column_statement;

SET @employee_cinema_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = 'user_db'
      AND table_name = 'employees'
      AND index_name = 'idx_employee_cinema'
);
SET @employee_cinema_index_sql = IF(
    @employee_cinema_index_exists = 0,
    'CREATE INDEX idx_employee_cinema ON user_db.employees(cinema_public_id, is_deleted, status)',
    'SELECT 1'
);
PREPARE employee_cinema_index_statement FROM @employee_cinema_index_sql;
EXECUTE employee_cinema_index_statement;
DEALLOCATE PREPARE employee_cinema_index_statement;

-- Dữ liệu demo vận hành: đội ngũ hiện tại làm việc tại LoraFilm Landmark 81.
UPDATE user_db.employees employee
JOIN user_db.users user_account ON user_account.account_id = employee.account_id
SET employee.cinema_public_id = 'b1575c2d-9081-11f1-bf65-0ebab02bf6f5'
WHERE user_account.email IN (
    'nhandt.ce190741@gmail.com',
    'nhannhinhanh63@gmail.com',
    'nhan15022022@gmail.com',
    'duongthanhphuong076@gmail.com'
);
