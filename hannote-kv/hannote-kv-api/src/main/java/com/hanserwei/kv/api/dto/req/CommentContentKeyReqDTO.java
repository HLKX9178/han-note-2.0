package com.hanserwei.kv.api.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评论正文复合主键请求项.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentContentKeyReqDTO {

    /** 发布年月，格式 yyyy-MM */
    @NotBlank(message = "发布年月不能为空")
    private String yearMonth;

    /** 评论正文 UUID */
    @NotBlank(message = "评论正文 ID 不能为空")
    private String contentId;
}
