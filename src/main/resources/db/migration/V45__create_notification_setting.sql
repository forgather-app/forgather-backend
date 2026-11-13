-- 호스트별 알림 설정 (#262)
-- 호스트와 1:1. 행이 없으면 허용(enabled = 1)으로 간주하고, PATCH /notifications/settings가 처음 호출될 때 행을 만든다.
-- 삭제 경로가 없는 설정 행이라 deleted_at을 두지 않는다 (notification_device와 같은 규칙). 탈퇴 시에도 행을 남긴다.
-- 종류별 설정이 필요해지면 컬럼을 추가한다.
-- host_id UNIQUE는 동시 최초 저장을 막는 장치다. 서비스는 이 제약 이름으로 위반을 판별해 새 트랜잭션에서 한 번 재시도한다.
CREATE TABLE `notification_setting`
(
    `id`         BIGINT     NOT NULL AUTO_INCREMENT,
    `host_id`    BIGINT     NOT NULL,
    `enabled`    TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `UK_notification_setting_host_id` UNIQUE (`host_id`)
);

ALTER TABLE `notification_setting`
    ADD CONSTRAINT `FK_host_TO_notification_setting` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`);
