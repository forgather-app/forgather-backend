ALTER TABLE `guest_book_report`
    DROP FOREIGN KEY `FK_guest_book_report_reason_TO_guest_book_report`;

ALTER TABLE `guest_book_report`
    DROP COLUMN `reason_id`;

ALTER TABLE `guest_book_report`
    ADD COLUMN `reason` VARCHAR(50) NOT NULL;

DROP TABLE `guest_book_report_reason`;
