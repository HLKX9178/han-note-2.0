package com.hanserwei.user.enums;

import com.hanserwei.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户服务响应码.
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("USER-10000", "出错啦，请稍后再试~"),

    // ----------- 业务异常状态码 -----------
    NICK_NAME_VALID_FAIL("USER-20001", "昵称请设置2-24个字符，不能使用@《/等特殊字符"),
    HANNOTE_ID_VALID_FAIL("USER-20002", "hannote 号请设置6-15个字符，仅可使用英文、数字、下划线"),
    SEX_VALID_FAIL("USER-20003", "性别错误"),
    INTRODUCTION_VALID_FAIL("USER-20004", "个人简介请设置1-100个字符"),
    UPLOAD_AVATAR_FAIL("USER-20005", "头像上传失败"),
    UPLOAD_BACKGROUND_IMG_FAIL("USER-20006", "背景图上传失败"),
    USER_NOT_FOUND("USER-20007", "用户不存在"),
    REGISTER_FAIL("USER-20008", "用户注册失败"),
    CANT_UPDATE_OTHER_USER_PROFILE("USER-20009", "无权限修改他人用户信息"),
    ;

    private final String errorCode;
    private final String errorMessage;
}
