USE `fa-cloud`;

DROP PROCEDURE IF EXISTS ensure_vip_user_phone_country_code;

DELIMITER //

CREATE PROCEDURE ensure_vip_user_phone_country_code()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'vip_user'
          AND column_name = 'phone_country_code'
    ) THEN
        ALTER TABLE vip_user
            ADD COLUMN phone_country_code varchar(8) DEFAULT NULL AFTER email;
    END IF;
END //

DELIMITER ;

CALL ensure_vip_user_phone_country_code();

DROP PROCEDURE IF EXISTS ensure_vip_user_phone_country_code;

UPDATE vip_user
SET phone_country_code = '+86'
WHERE phone IS NOT NULL
  AND phone <> ''
  AND (phone_country_code IS NULL OR phone_country_code = '');
