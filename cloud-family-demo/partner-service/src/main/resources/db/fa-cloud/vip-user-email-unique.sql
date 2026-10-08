USE `fa-cloud`;

ALTER TABLE vip_user
    ADD UNIQUE KEY uk_vip_user_email (email);
