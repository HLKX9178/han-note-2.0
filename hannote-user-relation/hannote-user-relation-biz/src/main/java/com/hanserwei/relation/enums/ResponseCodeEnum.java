package com.hanserwei.relation.enums;

import com.hanserwei.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户关系服务响应码.
 *
 * <p>错误码统一前缀 {@code RELATION-}。本期（��仅落地通用异常码，
 * 业务异常码待后续接口落地时按需补充（{@code RELATION-2xxxx}）。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("RELATION-10000", "出错啦，请稍后再试~"),
    PARAM_NOT_VALID("RELATION-10001", "参数错误"),

    // ----------- 业务异常状态码 -----------
    CANT_FOLLOW_YOUR_SELF("RELATION-20001", "无法关注自己"),
    FOLLOW_USER_NOT_EXISTED("RELATION-20002", "关注的用户不存在"),
    FOLLOWING_COUNT_LIMIT("RELATION-20003", "您关注的用户已达上限，请先取关部分用户"),
    ALREADY_FOLLOWED("RELATION-20004", "您已经关注了该用户"),
    CANT_UNFOLLOW_YOUR_SELF("RELATION-20005", "无法取关自己"),
    NOT_FOLLOWED("RELATION-20006", "你未关注对方，无法取关"),
    ;

    private final String errorCode;
    private final String errorMessage;
}
