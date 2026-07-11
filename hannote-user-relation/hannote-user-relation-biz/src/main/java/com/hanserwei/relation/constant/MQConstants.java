package com.hanserwei.relation.constant;

/**
 * 消息队列（RocketMQ）常量.
 *
 * <p>关注、取关属同类操作，共用一个 Topic，通过 Tag 二级分类区分具体操作类型。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
public interface MQConstants {

    /**
     * Topic 主题：关注 / 取关共用（集群模式消费，异步落库）
     */
    String TOPIC_FOLLOW_OR_UNFOLLOW = "FollowUnfollowTopic";

    /**
     * Tag 标签：关注
     */
    String TAG_FOLLOW = "Follow";

    /**
     * Tag 标签：取关
     */
    String TAG_UNFOLLOW = "Unfollow";

    /**
     * 生产者组
     */
    String GROUP_PRODUCER = "hannote_user_relation_group";

    /**
     * 关注 / 取关消费者组
     */
    String GROUP_FOLLOW_UNFOLLOW_CONSUMER = "hannote_user_relation_follow_unfollow_group";

    /**
     * Topic 主题：关注数计数（通知计数服务）
     */
    String TOPIC_COUNT_FOLLOWING = "CountFollowingTopic";

    /**
     * Topic 主题：粉丝数计数（通知计数服务）
     */
    String TOPIC_COUNT_FANS = "CountFansTopic";
}
