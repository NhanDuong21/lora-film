-- Normalize UUID and cross-service public-reference columns.
--
-- V1 used CHAR(36) for public identifiers. The runtime entities deliberately
-- map these values as VARCHAR(36), because the Auth/User/Score services now
-- use numeric account identifiers while legacy integrations still send UUIDs.
-- This additive migration converts existing CHAR(36) columns without changing
-- data or indexes and keeps upgrades from requiring a destructive re-create.

DELIMITER $$

DROP PROCEDURE IF EXISTS normalize_promotion_public_reference_columns_20260729$$
CREATE PROCEDURE normalize_promotion_public_reference_columns_20260729()
BEGIN
    DECLARE finished BOOLEAN DEFAULT FALSE;
    DECLARE table_name_value VARCHAR(128);
    DECLARE column_name_value VARCHAR(128);
    DECLARE nullable_value VARCHAR(3);
    DECLARE length_value INT;
    DECLARE default_value TEXT;
    DECLARE comment_value TEXT;
    DECLARE alter_sql TEXT;

    DECLARE char36_columns CURSOR FOR
        SELECT TABLE_NAME, COLUMN_NAME, IS_NULLABLE, CHARACTER_MAXIMUM_LENGTH,
               COLUMN_DEFAULT, COLUMN_COMMENT
        FROM information_schema.columns
        WHERE TABLE_SCHEMA = DATABASE()
          AND DATA_TYPE = 'char';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = TRUE;

    OPEN char36_columns;
    normalize_loop: LOOP
        FETCH char36_columns
            INTO table_name_value, column_name_value, nullable_value, length_value,
                 default_value, comment_value;
        IF finished THEN
            LEAVE normalize_loop;
        END IF;

        SET alter_sql = CONCAT(
            'ALTER TABLE `', REPLACE(table_name_value, '`', '``'),
            '` MODIFY COLUMN `', REPLACE(column_name_value, '`', '``'),
            '` VARCHAR(', length_value, ') ',
            IF(nullable_value = 'YES', 'NULL', 'NOT NULL'),
            IF(default_value IS NULL, '', CONCAT(' DEFAULT ', QUOTE(default_value))),
            IF(comment_value IS NULL OR comment_value = '',
               '',
               CONCAT(' COMMENT ', QUOTE(comment_value)))
        );

        SET @promotion_normalize_sql = alter_sql;
        PREPARE promotion_normalize_statement FROM @promotion_normalize_sql;
        EXECUTE promotion_normalize_statement;
        DEALLOCATE PREPARE promotion_normalize_statement;
    END LOOP;
    CLOSE char36_columns;
END$$

CALL normalize_promotion_public_reference_columns_20260729()$$
DROP PROCEDURE normalize_promotion_public_reference_columns_20260729$$

DELIMITER ;
