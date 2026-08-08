-- 호스트 프로필 사진을 Photo 엔티티로 전환 (#141)
-- host.picture_url 컬럼은 기존 데이터 보존을 위해 유지하되, 애플리케이션에서는 더 이상 사용하지 않는다.
-- soft delete로 같은 host_id에 여러 행(삭제된 행 N + 활성 행 1)이 존재하므로 host_id에 unique를 걸지 않는다.
CREATE TABLE `host_profile_photo`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `host_id`       BIGINT       NOT NULL,
    `original_name` VARCHAR(255) NOT NULL,
    `path`          VARCHAR(255) NOT NULL,
    `capacity`      BIGINT       NOT NULL,
    `created_at`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`    TIMESTAMP    NULL,
    PRIMARY KEY (`id`)
);

ALTER TABLE `host_profile_photo`
    ADD CONSTRAINT `FK_host_TO_host_profile_photo` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`);
