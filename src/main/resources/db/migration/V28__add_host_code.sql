-- 호스트 공개 식별자 추가 (#155)
-- 순차 PK 대신 외부에 노출할 불투명 코드. 알파벳/길이는 space.code 및 RandomCodeGenerator와 동일 ([0-9a-z] 10자)

-- 1) 컬럼 추가: nullable + 맨 뒤 → MySQL 8.0.12+ INSTANT DDL (테이블 재작성 없음)
ALTER TABLE `host`
    ADD COLUMN `code` VARCHAR(10) NULL;

-- 2) 기존 행 백필
--    RANDOM_BYTES()는 SSL 라이브러리 CSPRNG를 사용한다.
--    UUID()는 v1(시간+MAC 기반)이라 예측 가능하고 RAND()는 암호학적으로 안전하지 않아 쓰지 않는다.
--    64비트 난수를 36^10으로 나눈 나머지를 base36 10자리로 변환한다. CONV()는 대문자를 반환하므로 LOWER()로 맞춘다.
--    36^10 = 3656158440062976, 모듈로 편향은 2^64 / 36^10 ≈ 5046배라 무시 가능하다.
UPDATE `host`
SET `code` = LOWER(LPAD(CONV(
        CAST(CONV(HEX(RANDOM_BYTES(8)), 16, 10) AS UNSIGNED) MOD 3656158440062976,
        10, 36), 10, '0'))
WHERE `code` IS NULL;

-- 3) 충돌 정리: 충돌 확률은 사실상 0이지만, 4)의 UNIQUE 생성 실패를 막는 방어선
UPDATE `host` h
    JOIN (SELECT `code` FROM `host` GROUP BY `code` HAVING COUNT(*) > 1) dup
        ON h.`code` = dup.`code`
SET h.`code` = LOWER(LPAD(CONV(
        CAST(CONV(HEX(RANDOM_BYTES(8)), 16, 10) AS UNSIGNED) MOD 3656158440062976,
        10, 36), 10, '0'));

-- 4) 제약 확정: UNIQUE는 백필 뒤에 건다. 먼저 걸면 2)에서 충돌 시 전체가 롤백되어 재시도할 여지가 없다.
CREATE UNIQUE INDEX `UX_host_code` ON `host` (`code`);

ALTER TABLE `host`
    MODIFY COLUMN `code` VARCHAR(10) NOT NULL;
