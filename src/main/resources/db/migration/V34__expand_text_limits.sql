-- 표시 글자 수로 검증하는 복합 이모지의 저장 여유를 확보한다.
ALTER TABLE `space`
    MODIFY COLUMN `description` VARCHAR(5000) NOT NULL DEFAULT '';

ALTER TABLE `host`
    MODIFY COLUMN `nickname` VARCHAR(200) NULL;

-- nickname은 기존 VARCHAR(255)를 유지한다.
ALTER TABLE `guest_book_card`
    MODIFY COLUMN `message` VARCHAR(5000) NOT NULL;

ALTER TABLE `guest_book_report`
    MODIFY COLUMN `nickname_snapshot` VARCHAR(255) NOT NULL,
    MODIFY COLUMN `message_snapshot` VARCHAR(5000) NOT NULL;
