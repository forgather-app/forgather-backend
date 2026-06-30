CREATE TABLE `term`
(
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `type`       VARCHAR(50)  NOT NULL,
    `name`       VARCHAR(255) NOT NULL,
    `version`    VARCHAR(50)  NOT NULL,
    `content`    TEXT         NOT NULL,
    `sort_order` INT          NOT NULL,
    `is_required`   TINYINT(1)   NOT NULL,
    `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` TIMESTAMP NULL,
    PRIMARY KEY (`id`)
);
