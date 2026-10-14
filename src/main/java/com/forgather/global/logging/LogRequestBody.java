package com.forgather.global.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.slf4j.event.Level;

/**
 * 개발환경에서 해당 메서드의 요청 본문 원문을 기록한다.
 * 비밀번호나 토큰 등 민감정보가 포함된 API에는 사용하지 않는다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogRequestBody {

    Level level() default Level.DEBUG;
}
