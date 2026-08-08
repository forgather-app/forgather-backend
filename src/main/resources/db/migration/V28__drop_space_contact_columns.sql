-- 스페이스의 연락처 필드(이메일, 인스타그램 아이디)를 제거한다.
-- 호스트 이메일은 host 테이블이 보유하고(카카오 로그인), 외부 연결은 link_url/link_name이 담당하므로 더 이상 쓰지 않는다.
-- 되돌릴 수 없다. 배포 전 운영 DB 백업이 필요하다.
ALTER TABLE `space`
    DROP COLUMN `email`,
    DROP COLUMN `instagram_username`;
