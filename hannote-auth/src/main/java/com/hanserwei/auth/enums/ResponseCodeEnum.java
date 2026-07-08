package com.hanserwei.auth.enums;

import com.hanserwei.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 认证服务响应码枚举.
 *
 * <p>统一错误码分段规则：
 * <ul>
 *   <li>{@code AUTH-10000 ~ AUTH-19999}：通用 / 系统级错误；</li>
 *   <li>{@code AUTH-20000 ~ AUTH-29999}：验证码 / 登录 / 注册业务错误；</li>
 *   <li>后续业务可按段继续扩展。</li>
 * </ul>
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    SYSTEM_ERROR("AUTH-10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("AUTH-10001", "参数错误"),

    VERIFICATION_CODE_SEND_FREQUENTLY("AUTH-20000", "请求太频繁，请3分钟后再试"),
    VERIFICATION_CODE_ERROR("AUTH-20001", "验证码错误"),
    USER_NOT_FOUND("AUTH-20002", "用户不存在"),
    LOGIN_TYPE_NOT_SUPPORT("AUTH-20003", "暂不支持该登录方式"),
    UNAUTHORIZED("AUTH-20004", "未登录或登录已过期"),
    ACCOUNT_DISABLED("AUTH-20005", "账号已被禁用"),
    PHONE_OR_PASSWORD_ERROR("AUTH-20006", "手机号或密码错误"),
    LOGIN_FAIL("AUTH-20007", "登录失败");

    private final String errorCode;
    private final String errorMessage;
}
