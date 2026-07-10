package com.hanserwei.relation.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关注列表用户信息响应.
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FindFollowingUserRspVO {

    /** 用户 ID */
    private Long userId;

    /** 头像 */
    private String avatar;

    /** 昵称 */
    private String nickname;

    /** 个人简介 */
    private String introduction;
}
