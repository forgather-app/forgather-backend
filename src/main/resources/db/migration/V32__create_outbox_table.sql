-- 커밋 이후 후속 작업(커맨드)을 담는 공용 outbox 테이블
-- type은 새 작업 타입이 늘 때 DDL이 필요 없도록 VARCHAR 사용
-- status는 타입과 무관한 공용 어휘라 값이 늘 일이 거의 없어 ENUM으로 제약
CREATE TABLE `outbox`
(
    `id`         BIGINT                                  NOT NULL AUTO_INCREMENT,
    `type`       VARCHAR(50)                             NOT NULL,
    `status`     ENUM ('PENDING', 'COMPLETED', 'FAILED', 'CANCELED') NOT NULL,
    `payload`    TEXT                                    NULL,
    `fail_count` INT                                     NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP                               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP                               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);

CREATE INDEX `IX_outbox_type_status` ON `outbox` (`type`, `status`);
