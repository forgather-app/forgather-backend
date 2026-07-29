-- 호스트가 지정한 '지금 축하받고 있는 스페이스'
-- '호스트당 최대 1개' 제약은 애플리케이션 코드에서 보장한다.
ALTER TABLE `space`
    ADD COLUMN `is_celebrating` TINYINT(1) NOT NULL DEFAULT 0;
