ALTER TABLE `term`
    MODIFY COLUMN `version` VARCHAR(30) NOT NULL;

ALTER TABLE `term`
    ADD COLUMN `min_agreed_version` VARCHAR(30) NULL;

-- updated_at을 현재 값으로 명시해 자동 갱신(ON UPDATE)을 억제한다 (감사 타임스탬프 보존)
UPDATE `term`
SET `min_agreed_version` = `version`,
    `updated_at`         = `updated_at`;

ALTER TABLE `term`
    MODIFY COLUMN `min_agreed_version` VARCHAR(30) NOT NULL;
