-- 작품명 컬럼 확장
-- 컬럼 길이는 grapheme 검증 한도 × 10 (RGI 표준 최장 이모지 = 10 코드포인트)
ALTER TABLE `product`
    MODIFY COLUMN `title` VARCHAR(500) NOT NULL;
