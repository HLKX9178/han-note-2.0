package com.hanserwei.count.util;

import com.hanserwei.count.constant.MQConstants;
import com.hanserwei.count.enums.FollowUnfollowTypeEnum;
import com.hanserwei.count.model.dto.CountFollowUnfollowMqDTO;
import com.hanserwei.count.model.dto.FollowUnfollowSourceMqDTO;
import com.hanserwei.framework.common.util.JsonUtils;

import java.util.Objects;

/**
 * 关注/取关源事件解析器：把源 Topic 的消息（按 Tag 区分）归一化为计数用 {@link CountFollowUnfollowMqDTO}.
 *
 * <p>计数服务改为并行直消费源 Topic {@code FollowUnfollowTopic} 后，源消息体不再是
 * {@link CountFollowUnfollowMqDTO}，需按 Tag 解析对应源字段并推导 {@code type}，再交给原有计数逻辑。
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
public final class FollowUnfollowSourceParser {

    private FollowUnfollowSourceParser() {
    }

    /**
     * 按 Tag 将源消息体解析为归一化计数 DTO.
     *
     * @param tag  MQ Tag（{@link MQConstants#TAG_FOLLOW}/{@link MQConstants#TAG_UNFOLLOW}）
     * @param body 源消息体 JSON
     * @return 归一化 {@code CountFollowUnfollowMqDTO}；无法识别的 Tag 或解析失败返回 {@code null}
     */
    public static CountFollowUnfollowMqDTO parse(String tag, String body) {
        FollowUnfollowSourceMqDTO src = JsonUtils.parseObject(body, FollowUnfollowSourceMqDTO.class);
        if (Objects.isNull(src)) {
            return null;
        }
        if (Objects.equals(tag, MQConstants.TAG_FOLLOW)) {
            return CountFollowUnfollowMqDTO.builder()
                    .userId(src.getUserId())
                    .targetUserId(src.getFollowUserId())
                    .type(FollowUnfollowTypeEnum.FOLLOW.getCode())
                    .build();
        } else if (Objects.equals(tag, MQConstants.TAG_UNFOLLOW)) {
            return CountFollowUnfollowMqDTO.builder()
                    .userId(src.getUserId())
                    .targetUserId(src.getUnfollowUserId())
                    .type(FollowUnfollowTypeEnum.UNFOLLOW.getCode())
                    .build();
        }
        return null;
    }
}
