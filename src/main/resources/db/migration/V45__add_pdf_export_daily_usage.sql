ALTER TABLE space_pdf_export
    ADD COLUMN host_id BIGINT NULL;

CREATE INDEX idx_space_pdf_export_host_created_at
    ON space_pdf_export (host_id, created_at);
