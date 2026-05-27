package com.forgather.global.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Payload;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class TextSizeValidatorTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @DisplayName("isValid 단위 검증")
    @Nested
    class IsValidTest {

        @DisplayName("텍스트 길이가 min 이상 max 이하이면 검증을 통과한다.")
        @CsvSource(value = {
            "가나다,1,3",
            "가나다,3,3",
            "가나다,3,5",
            "abc,3,3"
        })
        @ParameterizedTest
        void isValidWithinRange(String text, int min, int max) {
            // given
            TextSizeValidator validator = newValidator(min, max);

            // when
            boolean result = validator.isValid(text, null);

            // then
            assertThat(result).isTrue();
        }

        @DisplayName("텍스트 길이가 max를 초과하면 검증에 실패한다.")
        @Test
        void isInvalidWhenExceedsMax() {
            // given
            TextSizeValidator validator = newValidator(0, 2);

            // when
            boolean result = validator.isValid("가나다", null);

            // then
            assertThat(result).isFalse();
        }

        @DisplayName("텍스트 길이가 min 미만이면 검증에 실패한다.")
        @Test
        void isInvalidWhenBelowMin() {
            // given
            TextSizeValidator validator = newValidator(3, 5);

            // when
            boolean result = validator.isValid("가나", null);

            // then
            assertThat(result).isFalse();
        }

        @DisplayName("이모지는 길이 1로 계산된다.")
        @Test
        void countsEmojiAsOne() {
            // given
            String emojis = "😀😀😀";
            TextSizeValidator validator = newValidator(0, 3);

            // when
            boolean result = validator.isValid(emojis, null);

            // then
            assertAll(
                () -> assertThat(emojis.length()).isEqualTo(6),
                () -> assertThat(result).isTrue()
            );
        }

        @DisplayName("null은 유효한 것으로 취급되어 TextLengthCounter 예외가 전파되지 않는다.")
        @Test
        void isValidWithNull() {
            // given
            TextSizeValidator validator = newValidator(1, 10);

            // when
            boolean result = validator.isValid(null, null);

            // then
            assertThat(result).isTrue();
        }

        @DisplayName("잘못된 제약 파라미터로 초기화하면 예외를 던진다.")
        @CsvSource(value = {
            "-1,10",
            "0,-1",
            "5,3"
        })
        @ParameterizedTest
        void throwExceptionWithInvalidParameters(int min, int max) {
            // when & then
            assertThatThrownBy(() -> newValidator(min, max))
                .isInstanceOf(IllegalArgumentException.class);
        }

        private TextSizeValidator newValidator(int min, int max) {
            TextSizeValidator validator = new TextSizeValidator();
            validator.initialize(textSize(min, max));
            return validator;
        }
    }

    @DisplayName("Validator 통합 검증")
    @Nested
    class ValidatorIntegrationTest {

        record Sample(
            @TextSize(
                min = 1,
                max = 3,
                message = "길이는 {min}~{max}자여야 합니다."
            )
            String value
        ) {
        }

        @DisplayName("필드가 제약을 만족하면 위반이 발생하지 않는다.")
        @ValueSource(strings = {"가", "가나다", "😀😀😀"})
        @ParameterizedTest
        void noViolationWhenValid(String value) {
            // given
            Sample sample = new Sample(value);

            // when
            Set<ConstraintViolation<Sample>> violations = validator.validate(sample);

            // then
            assertThat(violations).isEmpty();
        }

        @DisplayName("필드가 제약을 위반하면 위반이 발생한다.")
        @ValueSource(strings = {"", "가나다라"})
        @ParameterizedTest
        void violationWhenInvalid(String value) {
            // given
            Sample sample = new Sample(value);

            // when
            Set<ConstraintViolation<Sample>> violations = validator.validate(sample);

            // then
            assertThat(violations).hasSize(1);
        }

        @DisplayName("null 필드는 위반이 발생하지 않는다.")
        @Test
        void noViolationWhenNull() {
            // given
            Sample sample = new Sample(null);

            // when
            Set<ConstraintViolation<Sample>> violations = validator.validate(sample);

            // then
            assertThat(violations).isEmpty();
        }
    }

    @SuppressWarnings("unchecked")
    private static TextSize textSize(int min, int max) {
        return new TextSize() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return TextSize.class;
            }

            @Override
            public String message() {
                return "";
            }

            @Override
            public int min() {
                return min;
            }

            @Override
            public int max() {
                return max;
            }

            @Override
            public Class<?>[] groups() {
                return new Class<?>[0];
            }

            @Override
            public Class<? extends Payload>[] payload() {
                return (Class<? extends Payload>[])new Class<?>[0];
            }
        };
    }
}
