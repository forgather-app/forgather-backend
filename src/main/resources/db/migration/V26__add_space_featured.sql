-- 호스트가 지정한 '지금 축하받고 있는 스페이스'
-- 개수 제한이 없다. 한 호스트가 자신의 스페이스를 모두 지정할 수 있다.
ALTER TABLE `space`
    ADD COLUMN `is_featured` TINYINT(1) NOT NULL DEFAULT 0;
