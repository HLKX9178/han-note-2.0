package com.hanserwei.framework.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义手机号校验注解.
 *
 * <p>要求被注解的字符串为 11 位数字；允许为 {@code null}（此时由 {@code @NotBlank/@NotNull} 接管校验）。
 *
 * <p>用法示例：
 * <pre>
 * public class UserLoginReqVO {
 *     &#64;NotBlank
 *     &#64;PhoneNumber
 *     private String phone;
 * }
 * </pre>
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 * @see PhoneNumberValidator
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
public @interface PhoneNumber {

    String message() default "手机号格式不正确, 需为 11 位数字";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
