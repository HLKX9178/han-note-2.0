package com.hanserwei.user.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户主页信息返参.
 *
 * <p>计数字段用 {@link String}，经 {@code NumberUtils.formatNumberString} 格式化（如 "1.3万"）。
 *
 * @author hanserwei
 * @date 2026/07/17
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindUserProfileRspVO {

    /** 用户 ID */
    private Long userId;
    /** 头像 URL */
    private String avatar;
    /** 昵称 */
    private String nickname;
    /** 小憨书号 */
    private String hannoteId;
    /** 性别（0 女 / 1 男） */
    private Integer sex;
    /** 年龄（周岁） */
    private Integer age;
    /** 个人简介 */
    private String introduction;
    /** 关注数 */
    private String followingTotal;
    /** 粉丝数 */
    private String fansTotal;
    /** 点赞与收藏总数 */
    private String likeAndCollectTotal;
    /** 发布笔记数 */
    private String noteTotal;
    /** 获得点赞数 */
    private String likeTotal;
    /** 获得收藏数 */
    private String collectTotal;
}
