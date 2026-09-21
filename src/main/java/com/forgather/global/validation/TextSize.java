package com.forgather.global.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 문자열의 길이가 지정한 범위 내에 있는지 검증한다.
 * <p>
 * Jakarta {@code @Size}와 달리 {@code String.length()}(UTF-16 code unit)가 아니라
 * {@link com.forgather.global.util.TextLengthCounter}를 사용해 모든 문자(이모지 포함)를 1로 세는
 * grapheme(자소) 단위로 길이를 계산한다. 우리 서비스의 도메인 길이 규칙과 동일한 기준으로 요청 DTO를 fast-fail 검증하기 위함이다.
 * <p>
 * {@code null}은 유효한 것으로 취급한다(Jakarta {@code @Size} 표준 정책). 필수 값 검증은 {@code @NotNull},
 * {@code @NotBlank}와 조합한다.
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = TextSizeValidator.class)
@Documented
@Repeatable(TextSize.List.class)
public @interface TextSize {

    String message() default "길이는 {min}자 이상 {max}자 이하여야 합니다.";

    int min() default 0;

    int max() default Integer.MAX_VALUE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 같은 요소에 {@link TextSize}를 여러 번 적용하기 위한 컨테이너 어노테이션.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
    @Documented
    @Retention(RUNTIME)
    @interface List {

        TextSize[] value();
    }
}
