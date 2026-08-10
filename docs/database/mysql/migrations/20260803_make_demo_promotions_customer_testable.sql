-- Repair the E2E/demo promotion catalog so it is discoverable and evaluable
-- by the current customer checkout contract.
--
-- This migration intentionally targets only the stable demo promotion IDs.
-- It is safe to run more than once: rows are updated only when they still
-- differ from the canonical test configuration below.

USE promotion_db;

START TRANSACTION;

-- Public VOUCHER promotions are shown as claimable benefits to customers who
-- do not already own them. The legacy seed left this valid voucher private.
UPDATE promotions
SET is_public = TRUE,
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP(6),
    updated_by = 'demo-data-repair'
WHERE public_id = '1c91574b-8b84-58e9-896e-61073659407d'
  AND code = 'WELCOME10K'
  AND promotion_type = 'VOUCHER'
  AND deleted_at IS NULL
  AND is_public <> TRUE;

-- minimumTicketCount belonged to an old seed contract and is rejected by the
-- current Promotion Engine. Use the admin-supported minimumOrderAmount field.
UPDATE promotions
SET is_public = TRUE,
    conditions_json = JSON_OBJECT('minimumOrderAmount', 300000),
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP(6),
    updated_by = 'demo-data-repair'
WHERE public_id = '46ddec10-e1db-51a2-a9d6-e28ac4196e48'
  AND code = 'FAMILY30K'
  AND promotion_type = 'VOUCHER'
  AND deleted_at IS NULL
  AND (
      is_public <> TRUE
      OR JSON_LENGTH(conditions_json) <> 1
      OR COALESCE(
          CAST(JSON_UNQUOTE(
              JSON_EXTRACT(conditions_json, '$.minimumOrderAmount')
          ) AS DECIMAL(19, 2)),
          -1
      ) <> 300000
  );

-- dayType/timeBefore and maxDiscount came from an obsolete seed shape. Keep
-- the advertised weekday behavior with fields accepted by the current engine.
UPDATE promotions
SET conditions_json = JSON_OBJECT(
        'showtimeDayOfWeek',
        JSON_ARRAY('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY')
    ),
    actions_json = JSON_OBJECT(
        'discountType', 'PERCENTAGE',
        'discountValue', 10,
        'maxDiscountAmount', 30000
    ),
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP(6),
    updated_by = 'demo-data-repair'
WHERE public_id = 'b6393715-e387-596f-a8f0-d9af17a5678f'
  AND promotion_type = 'AUTO'
  AND deleted_at IS NULL
  AND (
      JSON_LENGTH(conditions_json) <> 1
      OR JSON_LENGTH(
          JSON_EXTRACT(conditions_json, '$.showtimeDayOfWeek')
      ) <> 5
      OR NOT JSON_CONTAINS(
          JSON_EXTRACT(conditions_json, '$.showtimeDayOfWeek'),
          JSON_QUOTE('MONDAY')
      )
      OR NOT JSON_CONTAINS(
          JSON_EXTRACT(conditions_json, '$.showtimeDayOfWeek'),
          JSON_QUOTE('TUESDAY')
      )
      OR NOT JSON_CONTAINS(
          JSON_EXTRACT(conditions_json, '$.showtimeDayOfWeek'),
          JSON_QUOTE('WEDNESDAY')
      )
      OR NOT JSON_CONTAINS(
          JSON_EXTRACT(conditions_json, '$.showtimeDayOfWeek'),
          JSON_QUOTE('THURSDAY')
      )
      OR NOT JSON_CONTAINS(
          JSON_EXTRACT(conditions_json, '$.showtimeDayOfWeek'),
          JSON_QUOTE('FRIDAY')
      )
      OR JSON_LENGTH(actions_json) <> 3
      OR JSON_UNQUOTE(JSON_EXTRACT(actions_json, '$.discountType'))
          <> 'PERCENTAGE'
      OR COALESCE(
          CAST(JSON_UNQUOTE(
              JSON_EXTRACT(actions_json, '$.discountValue')
          ) AS DECIMAL(19, 2)),
          -1
      ) <> 10
      OR COALESCE(
          CAST(JSON_UNQUOTE(
              JSON_EXTRACT(actions_json, '$.maxDiscountAmount')
          ) AS DECIMAL(19, 2)),
          -1
      ) <> 30000
  );

COMMIT;
