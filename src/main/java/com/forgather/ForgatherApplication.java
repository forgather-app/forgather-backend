package com.forgather;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan
public class ForgatherApplication {

	private static final String DEFAULT_TIME_ZONE = "Asia/Seoul";

	public static void main(String[] args) {
		// LocalDateTime(created_at 등)은 JVM 기본 타임존으로 기록된다. 서버 OS 설정과 무관하게 KST로 고정한다.
		TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_TIME_ZONE));
		SpringApplication.run(ForgatherApplication.class, args);
	}
}
