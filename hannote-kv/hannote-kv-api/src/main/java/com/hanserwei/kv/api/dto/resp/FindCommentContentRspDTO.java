package com.hanserwei.kv.api.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评论正文查询响应项.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindCommentContentRspDTO {

    private String contentId;
    private String content;
}
