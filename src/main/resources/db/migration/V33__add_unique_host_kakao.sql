ALTER TABLE `host_kakao`
    ADD CONSTRAINT `UK_host_kakao_host_id` UNIQUE (`host_id`),
    ADD CONSTRAINT `UK_host_kakao_user_id` UNIQUE (`user_id`);
