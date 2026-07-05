ALTER TABLE `host_kakao`
    ADD COLUMN `name` VARCHAR(255) NULL;

UPDATE `host_kakao` hk
    JOIN `host` h ON h.`id` = hk.`host_id`
SET hk.`name` = h.`name`;

ALTER TABLE `host_kakao`
    MODIFY COLUMN `name` VARCHAR(255) NOT NULL;

ALTER TABLE `host`
    MODIFY COLUMN `name` VARCHAR(20) NULL;

CREATE TABLE `host_term_history`
(
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `host_id`    BIGINT       NOT NULL,
    `term_id`    BIGINT       NOT NULL,
    `action`     VARCHAR(50)  NOT NULL,
    `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` TIMESTAMP NULL,
    PRIMARY KEY (`id`)
);

ALTER TABLE `host_term_history`
    ADD CONSTRAINT `FK_host_TO_host_term_history` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`);

ALTER TABLE `host_term_history`
    ADD CONSTRAINT `FK_term_TO_host_term_history` FOREIGN KEY (`term_id`) REFERENCES `term` (`id`);

ALTER TABLE `host`
    DROP COLUMN `agreed_terms`;
