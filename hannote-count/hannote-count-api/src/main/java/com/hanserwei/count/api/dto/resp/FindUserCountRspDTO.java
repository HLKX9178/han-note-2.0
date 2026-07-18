package com.hanserwei.count.api.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户维度计数响应.
 *
 * @author hanserwei
 * @date 2026/07/17
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindUserCountRspDTO {

    /** 用户 ID */
    private Long userId;

    /** 粉丝数 */
    private Long fansTotal;

    /** 关注数 */
    private Long followingTotal;

    /** 发布笔记数 */
    private Long noteTotal;

    /** 获得点赞数 */
    private Long likeTotal;

    /** 获得收藏数 */
    private Long collectTotal;
}
