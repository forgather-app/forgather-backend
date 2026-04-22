ALTER TABLE `guest_book_card`
    ADD COLUMN `visibility_status` ENUM ('VISIBLE', 'HIDDEN_BY_USER', 'HIDDEN_BY_ADMIN') NOT NULL DEFAULT 'VISIBLE';

CREATE TABLE `guest_book_report_reason`
(
    `id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `code`          VARCHAR(30) NOT NULL,
    `label`         VARCHAR(30) NOT NULL,
    `display_order` INT         NOT NULL,
    `is_hidden`     TINYINT(1)   NOT NULL,
    `created_at`    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);

CREATE UNIQUE INDEX `UX_guest_book_report_reason_code` ON `guest_book_report_reason` (`code`);

CREATE TABLE `guest_book_report`
(
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
    `guest_book_card_id`  BIGINT       NOT NULL,
    `host_user_id`        BIGINT       NOT NULL,
    `reporter_user_id`    BIGINT       NOT NULL,
    `reason_id`           BIGINT       NOT NULL,
    `reporter_type`       ENUM ('HOST') NOT NULL,
    `detail`              VARCHAR(500) NULL,
    `nickname_snapshot`   VARCHAR(10)  NOT NULL,
    `message_snapshot`    VARCHAR(500) NOT NULL,
    `created_at_snapshot` TIMESTAMP    NOT NULL,
    `created_at`          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);

ALTER TABLE `guest_book_report`
    ADD CONSTRAINT `FK_guest_book_card_TO_guest_book_report`
        FOREIGN KEY (`guest_book_card_id`) REFERENCES `guest_book_card` (`id`);

ALTER TABLE `guest_book_report`
    ADD CONSTRAINT `FK_host_TO_guest_book_report_host_user`
        FOREIGN KEY (`host_user_id`) REFERENCES `host` (`id`);

ALTER TABLE `guest_book_report`
    ADD CONSTRAINT `FK_host_TO_guest_book_report_reporter_user`
        FOREIGN KEY (`reporter_user_id`) REFERENCES `host` (`id`);

ALTER TABLE `guest_book_report`
    ADD CONSTRAINT `FK_guest_book_report_reason_TO_guest_book_report`
        FOREIGN KEY (`reason_id`) REFERENCES `guest_book_report_reason` (`id`);
