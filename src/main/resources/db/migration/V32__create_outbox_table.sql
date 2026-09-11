-- 커밋 이후 후속 작업(커맨드)을 담는 공용 outbox 테이블
-- type/status는 새 작업 타입 추가 시 DDL이 필요 없도록 ENUM 대신 VARCHAR 사용
CREATE TABLE `outbox`
(
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `type`       VARCHAR(50) NOT NULL,
    `status`     VARCHAR(20) NOT NULL,
    `payload`    JSON        NULL,
    `fail_count` INT         NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);

CREATE INDEX `IX_outbox_type_status` ON `outbox` (`type`, `status`);
