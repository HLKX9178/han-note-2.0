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
    SYSTEM_ERROR("COMMENT-10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("COMMENT-10001", "参数错误"),

    // ---------- 业务 ----------
    REPLY_COMMENT_NOT_FOUND("COMMENT-20001", "回复的评论不存在"),
    COMMENT_NOT_FOUND("COMMENT-20002", "评论不存在"),
    COMMENT_ALREADY_LIKED("COMMENT-20003", "评论已点赞"),
    COMMENT_NOT_LIKED("COMMENT-20004", "评论未点赞"),
    COMMENT_OPERATION_FORBIDDEN("COMMENT-20005", "无权操作该评论"),
    PARENT_COMMENT_INVALID("COMMENT-20006", "父评论状态异常"),
    NOTE_NOT_FOUND("COMMENT-20007", "笔记不存在或已删除"),
    REPLY_COMMENT_NOTE_MISMATCH("COMMENT-20008", "不能跨笔记回复评论"),
    ;

    private final String errorCode;
    private final String errorMessage;
}
