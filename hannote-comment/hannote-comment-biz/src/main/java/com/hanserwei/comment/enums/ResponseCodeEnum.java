package com.hanserwei.comment.enums;

import com.hanserwei.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评论服务响应异常码.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ---------- 通用 ----------
    /** 系统未捕获异常，兜底返回 */
    SYSTEM_ERROR("COMMENT-10000", "出错啦，后台小哥正在努力修复中..."),
    /** 请求参数校验不通过 */
    PARAM_NOT_VALID("COMMENT-10001", "参数错误"),

    // ---------- 业务 ----------
    /** 回复的目标评论不存在 */
    REPLY_COMMENT_NOT_FOUND("COMMENT-20001", "回复的评论不存在"),
    /** 评论不存在 */
    COMMENT_NOT_FOUND("COMMENT-20002", "评论不存在"),
    /** 重复点赞已点赞过的评论 */
    COMMENT_ALREADY_LIKED("COMMENT-20003", "评论已点赞"),
    /** 取消点赞时评论并未点赞 */
    COMMENT_NOT_LIKED("COMMENT-20004", "评论未点赞"),
    /** 非本人评论，无权删除等操作 */
    COMMENT_OPERATION_FORBIDDEN("COMMENT-20005", "无权操作该评论"),
    /** 父评论已删除或状态异常，无法回复 */
    PARENT_COMMENT_INVALID("COMMENT-20006", "父评论状态异常"),
    /** 目标笔记不存在或已删除 */
    NOTE_NOT_FOUND("COMMENT-20007", "笔记不存在或已删除"),
    /** 回复的评论与当前笔记不属于同一篇 */
    REPLY_COMMENT_NOTE_MISMATCH("COMMENT-20008", "不能跨笔记回复评论"),
    ;

    /** 错误码（前缀 COMMENT） */
    private final String errorCode;
    /** 面向用户的错误提示 */
    private final String errorMessage;
}
