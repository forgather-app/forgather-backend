-- 공통 알림 도메인 (#226)
-- type, platform, target_type은 값이 늘 때 DDL이 필요 없도록 VARCHAR 사용
-- dedupe_key는 스케줄 생성 알림의 중복 실행 방지 키. 이벤트 기반 알림은 NULL이며 UNIQUE는 NULL 중복을 허용한다.
-- sent_at은 공급자가 전송 요청을 접수한 시각이다. 단말 도달 시각이 아니다.
-- title·body는 표시 글자 수(제목 100자, 본문 500자)로 검증하므로 복합 이모지 저장 여유를 위해 컬럼은 10배로 잡는다 (V34와 같은 규칙).
CREATE TABLE `notification`
(
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `host_id`     BIGINT        NOT NULL,
    `type`        VARCHAR(50)   NOT NULL,
    `title`       VARCHAR(1000) NOT NULL,
    `body`        VARCHAR(5000) NOT NULL,
    `target_type` VARCHAR(30)   NULL,
    `target_key`  VARCHAR(255)  NULL,
    `dedupe_key`  VARCHAR(255)  NULL,
    `read_at`     TIMESTAMP     NULL,
    `sent_at`     TIMESTAMP     NULL,
    `created_at`  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`  TIMESTAMP     NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `UK_notification_dedupe_key` UNIQUE (`dedupe_key`)
);

-- 호스트별 알림함 조회용
CREATE INDEX `IX_notification_host_id_created_at` ON `notification` (`host_id`, `created_at`);
-- 다이제스트의 "같은 대상에 마지막으로 생성한 알림" 조회용 (created_at 기준. 실제 쿼리는 리포지토리 단계에서 확정)
CREATE INDEX `IX_notification_type_target_created_at`
    ON `notification` (`type`, `target_type`, `target_key`, `created_at`);

ALTER TABLE `notification`
    ADD CONSTRAINT `FK_host_TO_notification` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`);

-- 푸시 디바이스 토큰. 토큰은 호스트가 아니라 기기에 속하므로 호스트 테이블과 분리한다.
-- token은 soft delete된 행까지 포함해 UNIQUE. 같은 토큰이 다시 등록되면 기존 행의 소유자를 바꾸고 복구한다.
CREATE TABLE `notification_device`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `host_id`        BIGINT       NOT NULL,
    `platform`       VARCHAR(20)  NOT NULL,
    `token`          VARCHAR(512) NOT NULL,
    `last_active_at` TIMESTAMP    NOT NULL,
    `created_at`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`     TIMESTAMP    NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `UK_notification_device_token` UNIQUE (`token`)
);

CREATE INDEX `IX_notification_device_host_id` ON `notification_device` (`host_id`);

ALTER TABLE `notification_device`
    ADD CONSTRAINT `FK_host_TO_notification_device` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`);
