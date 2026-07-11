package com.hanserwei.count.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户计数表数据对象.
 *
 * <p>对应数据表 {@code t_user_count}。按用户维度维护粉丝数、关注数、发布笔记数、
 * 获赞数、获收藏数，随关注/取关、发布/删除笔记、点赞/收藏动作增量维护，
 * 避免实时 {@code COUNT} 聚合查询。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_user_count")
public class UserCountDO {

    /** 主键 ID（库自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 粉丝总数 */
    private Long fansTotal;

    /** 关注总数 */
    private Long followingTotal;

    /** 发布笔记总数 */
    private Long noteTotal;

    /** 获得点赞总数 */
    private Long likeTotal;

    /** 获得收藏总数 */
    private Long collectTotal;
}
