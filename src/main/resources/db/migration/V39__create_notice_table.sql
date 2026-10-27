CREATE TABLE `notice`
(
    `id`         BIGINT        NOT NULL AUTO_INCREMENT,
    `title`      VARCHAR(1000) NOT NULL,
    `body`       TEXT          NOT NULL,
    `created_at` TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` TIMESTAMP NULL,
    PRIMARY KEY (`id`)
);
