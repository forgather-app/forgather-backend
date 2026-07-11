ALTER TABLE `host`
    ADD COLUMN `email` VARCHAR(255) NULL;

CREATE TABLE `host_apple`
(
    `id`      BIGINT       NOT NULL AUTO_INCREMENT,
    `host_id` BIGINT       NOT NULL,
    `user_id` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
);

ALTER TABLE `host_apple`
    ADD CONSTRAINT `FK_host_TO_host_apple` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`);
