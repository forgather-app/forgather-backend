CREATE TABLE `exhibition`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `title`            VARCHAR(255) NOT NULL,
    `start_date`       DATE         NOT NULL,
    `end_date`         DATE         NOT NULL,
    `description`      VARCHAR(255) NULL,
    `operation_notice` VARCHAR(255) NULL,
    `location_type`    ENUM ('ONLINE', 'OFFLINE') NULL,
    `online_url`       VARCHAR(255) NULL,
    `base_address`     VARCHAR(255) NULL,
    `detail_address`   VARCHAR(255) NULL,
    `created_at`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `exhibition_host`
(
    `id`            BIGINT    NOT NULL AUTO_INCREMENT,
    `exhibition_id` BIGINT    NOT NULL,
    `host_id`       BIGINT    NOT NULL,
    `is_creator`    TINYINT(1) NOT NULL,
    `created_at`    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`    TIMESTAMP NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `exhibition_photo`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `exhibition_id` BIGINT       NOT NULL,
    `original_name` VARCHAR(255) NOT NULL,
    `path`          VARCHAR(255) NOT NULL,
    `capacity`      BIGINT       NOT NULL,
    `created_at`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`    TIMESTAMP NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `exhibition_time`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `exhibition_id` BIGINT       NOT NULL,
    `day_type`      VARCHAR(255) NOT NULL,
    `start_time`    TIME         NOT NULL,
    `end_time`      TIME         NOT NULL,
    `created_at`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`    TIMESTAMP NULL,
    PRIMARY KEY (`id`)
);

ALTER TABLE `exhibition_host`
    ADD CONSTRAINT `FK_exhibition_TO_exhibition_host`
        FOREIGN KEY (`exhibition_id`) REFERENCES `exhibition` (`id`);

ALTER TABLE `exhibition_host`
    ADD CONSTRAINT `FK_host_TO_exhibition_host`
        FOREIGN KEY (`host_id`) REFERENCES `host` (`id`);

ALTER TABLE `exhibition_photo`
    ADD CONSTRAINT `FK_exhibition_TO_exhibition_photo`
        FOREIGN KEY (`exhibition_id`) REFERENCES `exhibition` (`id`);

ALTER TABLE `exhibition_time`
    ADD CONSTRAINT `FK_exhibition_TO_exhibition_time`
        FOREIGN KEY (`exhibition_id`) REFERENCES `exhibition` (`id`);
