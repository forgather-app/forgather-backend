package com.forgather.global.validation;

import com.forgather.global.util.TextLengthCounter;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link TextSize} 제약을 검증하는 validator.
 * <p>
 * {@link TextLengthCounter#count(String)} 기반으로 grapheme(자소) 단위 길이를 계산해
 * {@code min}, {@code max} 범위 검증을 수행한다.
 */
public class TextSizeValidator implements ConstraintValidator<TextSize, CharSequence> {

    private int min;
    private int max;

    @Override
    public void initialize(TextSize constraint) {
        this.min = constraint.min();
        this.max = constraint.max();
        validateConstraintParameters();
    }

    private void validateConstraintParameters() {
        if (min < 0) {
            throw new IllegalArgumentException("최소 길이는 음수일 수 없습니다: " + min);
        }
        if (max < 0) {
            throw new IllegalArgumentException("최대 길이는 음수일 수 없습니다: " + max);
        }
        if (min > max) {
            throw new IllegalArgumentException("최소 길이는 최대 길이보다 클 수 없습니다: min=" + min + ", max=" + max);
        }
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int length = TextLengthCounter.count(value.toString());
        return min <= length && length <= max;
    }
}
