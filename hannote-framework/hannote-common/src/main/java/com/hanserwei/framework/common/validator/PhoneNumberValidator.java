package com.hanserwei.framework.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 手机号校验器.
 *
 * <p>实现 {@link PhoneNumber} 注解的校验逻辑：字符串必须为 11 位数字；
 * {@code null} 值直接放行，由 {@code @NotBlank} / {@code @NotNull} 接管校验。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

    private static final String PHONE_REGEX = "\\d{11}";

    @Override
    public void initialize(PhoneNumber constraintAnnotation) {
        // no-op
    }

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        // 允许为 null 时由 @NotBlank/@NotNull 接管校验
        if (phoneNumber == null) {
            return true;
        }
        return phoneNumber.matches(PHONE_REGEX);
    }
}
