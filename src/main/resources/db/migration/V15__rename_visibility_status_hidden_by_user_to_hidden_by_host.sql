ALTER TABLE `guest_book_card`
    MODIFY COLUMN `visibility_status`
        ENUM ('VISIBLE', 'HIDDEN_BY_HOST', 'HIDDEN_BY_ADMIN') NOT NULL DEFAULT 'VISIBLE';
