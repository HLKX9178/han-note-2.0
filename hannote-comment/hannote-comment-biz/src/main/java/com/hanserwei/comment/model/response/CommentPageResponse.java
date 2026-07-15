package com.hanserwei.comment.model.response;

import com.hanserwei.framework.common.response.PageResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 一级评论分页响应，额外携带笔记全部评论数.
 *
 * @param <T> 评论项类型
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommentPageResponse<T> extends PageResponse<T> {

    private long commentTotal;

    /**
     * 构建成功响应.
     */
    public static <T> CommentPageResponse<T> success(List<T> data, long pageNo,
                                                      long rootTotal, long pageSize,
                                                      long commentTotal) {
        CommentPageResponse<T> response = new CommentPageResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setPageNo(pageNo);
        response.setTotalCount(rootTotal);
        response.setPageSize(pageSize);
        response.setTotalPage(PageResponse.getTotalPage(rootTotal, pageSize));
        response.setCommentTotal(commentTotal);
        return response;
    }
}
