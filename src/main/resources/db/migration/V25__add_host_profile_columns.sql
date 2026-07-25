-- 마이 프로필: 한 줄 소개, 링크 추가 및 닉네임 컬럼 확장 (#138)
-- 컬럼 길이는 grapheme 검증 한도 × 10 (RGI 표준 최장 이모지 = 10 코드포인트)
ALTER TABLE `host`
    ADD COLUMN `introduction` VARCHAR(500) NULL,
    ADD COLUMN `link_url`     VARCHAR(2048) NULL,
    MODIFY COLUMN `nickname`  VARCHAR(100) NULL;
