-- Recover lineage for clones created by the legacy POST /clone implementation.
UPDATE promotions copied
JOIN promotions origin
  ON origin.campaign_public_id = copied.campaign_public_id
 AND origin.promotion_type = 'VOUCHER'
 AND copied.promotion_type = 'VOUCHER'
 AND copied.name = CONCAT(origin.name, ' (Copy)')
 AND CHAR_LENGTH(copied.code) = CHAR_LENGTH(origin.code) + 12
 AND LEFT(UPPER(copied.code), CHAR_LENGTH(origin.code) + 6)
     = CONCAT(UPPER(origin.code), '_COPY_')
SET copied.cloned_from_public_id = origin.public_id
WHERE copied.cloned_from_public_id IS NULL
  AND copied.deleted_at IS NULL
  AND origin.deleted_at IS NULL;

-- Public voucher clones are separate claimable promotions and must remain public.
UPDATE promotions copied
JOIN promotions origin
  ON origin.public_id = copied.cloned_from_public_id
SET copied.is_public = TRUE
WHERE copied.promotion_type = 'VOUCHER'
  AND origin.promotion_type = 'VOUCHER'
  AND copied.is_public = FALSE
  AND origin.is_public = TRUE
  AND copied.deleted_at IS NULL
  AND origin.deleted_at IS NULL;
