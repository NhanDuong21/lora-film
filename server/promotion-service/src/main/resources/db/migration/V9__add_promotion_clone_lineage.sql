ALTER TABLE promotions
    ADD COLUMN cloned_from_public_id VARCHAR(36) NULL;

CREATE INDEX idx_promotions_cloned_from
    ON promotions (cloned_from_public_id);
