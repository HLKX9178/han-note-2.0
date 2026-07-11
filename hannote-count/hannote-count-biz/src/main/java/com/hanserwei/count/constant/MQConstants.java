package com.hanserwei.count.constant;

/**
 * 消息队列（RocketMQ）常量.
 *
 * <p>计数服务消费 4 个 topic：关注数/粉丝数计数（写 Redis），以及各自的落库 topic。
 * 每个 topic 独占一个 consumer group，避免同组订阅多 topic 导致的负载均衡异常。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public interface MQConstants {

    /** Topic：关注数计数（写 Redis） */
    String TOPIC_COUNT_FOLLOWING = "CountFollowingTopic";

    /** Topic：粉丝数计数（写 Redis） */
    String TOPIC_COUNT_FANS = "CountFansTopic";

    /** Topic：粉丝数计数落库 */
    String TOPIC_COUNT_FANS_2_DB = "CountFans2DBTopic";

    /** Topic：关注数计数落库 */
    String TOPIC_COUNT_FOLLOWING_2_DB = "CountFollowing2DBTopic";

    /** 消费者组：关注数计数 */
    String GROUP_COUNT_FOLLOWING = "hannote_count_following_group";

    /** 消费者组：粉丝数计数 */
    String GROUP_COUNT_FANS = "hannote_count_fans_group";

    /** 消费者组：粉丝数计数落库 */
    String GROUP_COUNT_FANS_2_DB = "hannote_count_fans_2db_group";

    /** 消费者组：关注数计数落库 */
    String GROUP_COUNT_FOLLOWING_2_DB = "hannote_count_following_2db_group";
}
