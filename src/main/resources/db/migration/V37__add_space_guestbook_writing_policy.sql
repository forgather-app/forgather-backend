ALTER TABLE `space`
    ADD COLUMN `guest_book_writing_policy` VARCHAR(32) NOT NULL DEFAULT 'ALWAYS';
