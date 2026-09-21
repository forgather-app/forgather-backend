ALTER TABLE `host`
    ADD COLUMN `deleted_at` TIMESTAMP NULL,
    ADD COLUMN `anonymized_at` TIMESTAMP NULL;

CREATE TABLE `social_revoke_fail_log`
(
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
    `provider`            ENUM ('KAKAO', 'GOOGLE', 'APPLE') NOT NULL,
    `social_user_id`      VARCHAR(255) NOT NULL,
    `apple_refresh_token` VARCHAR(512) NULL,
    `fail_count`          INT          NOT NULL DEFAULT 1,
    `completed_at`        TIMESTAMP    NULL,
    `created_at`          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);
