CREATE TABLE space_pdf_export (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_id VARCHAR(255) NOT NULL,
    snapshot JSON NOT NULL,
    email VARCHAR(320) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_space_pdf_export_job_id UNIQUE (job_id)
);
