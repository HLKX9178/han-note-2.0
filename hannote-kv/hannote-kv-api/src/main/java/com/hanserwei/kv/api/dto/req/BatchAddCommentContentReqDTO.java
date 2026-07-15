package com.hanserwei.kv.api.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量新增评论内容请求（服务间调用）.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAddCommentContentReqDTO {

    /** 评论内容集合（非空，元素逐个校验） */
    @NotEmpty(message = "评论内容集合不能为空")
    @Valid
    private List<CommentContentReqDTO> comments;
}
