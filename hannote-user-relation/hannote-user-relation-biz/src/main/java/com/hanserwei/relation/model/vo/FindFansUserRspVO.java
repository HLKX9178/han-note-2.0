package com.hanserwei.relation.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 粉丝列表用户信息响应.
 *
 * <p>相比关注列表，粉丝信息额外展示粉丝总数与笔记总数。计数服务尚未建设，
 * 本期 {@code fansTotal} / {@code noteTotal} 暂固定为 0。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FindFansUserRspVO {

    /** 用户 ID */
    private Long userId;

    /** 头像 */
    private String avatar;

    /** 昵称 */
    private String nickname;

    /** 粉丝总数 */
    private Long fansTotal;

    /** 笔记总数 */
    private Long noteTotal;
}
