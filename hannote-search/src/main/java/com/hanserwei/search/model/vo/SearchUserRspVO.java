package com.hanserwei.search.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索用户返参.
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchUserRspVO {

    /** 用户 ID */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 昵称：关键词高亮（{@code <em>} 包裹） */
    private String highlightNickname;

    /** 头像 */
    private String avatar;

    /** 小憨书号 */
    private String hannoteId;

    /** 发布笔记总数 */
    private Integer noteTotal;

    /** 粉丝总数（格式化展示，如 {@code 13.7万}） */
    private String fansTotal;
}
