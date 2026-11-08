CREATE INDEX `IX_guest_book_card_space_visibility_latest`
    ON `guest_book_card` (`space_id`, `visibility_status`, `deleted_at`, `created_at` DESC, `id` DESC);
