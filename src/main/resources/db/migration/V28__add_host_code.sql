-- 순차 PK 대신 외부에 노출할 불투명 코드. 길이는 10자이고 문자 집합은 [0-9a-z]

-- 1) 컬럼 추가
ALTER TABLE `host`
    ADD COLUMN `code` VARCHAR(10) NULL;

-- 2) 임시 DEFAULT를 먼저 건다
--    블루/그린 배포에서 이 마이그레이션은 새 슬롯 부팅 시점(ApplicationStart)에 커밋되지만,
--    nginx 전환은 다음 훅(ValidateService)이라 그 사이 트래픽은 code를 모르는 구 버전 jar가 받는다.
--    구 버전 Hibernate는 INSERT에서 code를 통째로 빼는데, DEFAULT가 없어 신규 가입이 전부 실패하게 된다.
ALTER TABLE `host`
    ALTER COLUMN `code` SET DEFAULT (LOWER(HEX(RANDOM_BYTES(5))));

-- 3) 기존 행 백필
--    RANDOM_BYTES()는 SSL 라이브러리 CSPRNG를 사용한다.
--    UUID()는 v1(시간+MAC 기반)이라 예측 가능하고 RAND()는 암호학적으로 안전하지 않아 쓰지 않는다.
--    HEX()는 대문자를 반환하므로 LOWER()로 [0-9a-f]에 맞춘다.
--    마이그레이션 이후 신규 가입은 RandomCodeGenerator가 [0-9a-z]으로 만든다.
UPDATE `host`
SET `code` = LOWER(HEX(RANDOM_BYTES(5)))
WHERE `code` IS NULL;

-- 4) 충돌 정리: 충돌 확률은 사실상 0이지만, 5)의 UNIQUE 생성 실패를 막는 방어선
UPDATE `host` h
    JOIN (
        SELECT `code`
        FROM `host`
        GROUP BY `code`
        HAVING COUNT(*) > 1
    ) dup
        ON h.`code` = dup.`code`
SET h.`code` = LOWER(HEX(RANDOM_BYTES(5)));

-- 5) 제약 확정: UNIQUE는 백필 뒤에 건다. 먼저 걸면 3)에서 충돌 시 전체가 롤백되어 재시도할 여지가 없다.
CREATE UNIQUE INDEX `UX_host_code` ON `host` (`code`);

-- 6) NOT NULL 확정.
--    배포가 안정되면 후속 마이그레이션에서 DROP DEFAULT 한다.
ALTER TABLE `host`
    MODIFY COLUMN `code` VARCHAR(10) NOT NULL DEFAULT (LOWER(HEX(RANDOM_BYTES(5))));
