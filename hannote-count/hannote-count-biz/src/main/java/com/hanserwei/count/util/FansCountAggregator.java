package com.hanserwei.count.util;

import com.hanserwei.count.enums.FollowUnfollowTypeEnum;
import com.hanserwei.count.model.dto.CountFollowUnfollowMqDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 粉丝数聚合计算.
 *
 * <p>将一批关注/取关消息按目标用户分组，关注 +1、取关 -1 净算，得到每个目标用户
 * 本批次的粉丝数增量。纯函数、无副作用，便于脱离 Reactor / MQ 单测。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public final class FansCountAggregator {

    private FansCountAggregator() {
    }

    /**
     * 按目标用户聚合粉丝数增量。
     *
     * @param dtoList 一批关注/取关消息
     * @return key 为目标用户 ID，value 为净增量（可正可负）；入参为空返回空 Map
     */
    public static Map<Long, Integer> aggregate(List<CountFollowUnfollowMqDTO> dtoList) {
        Map<Long, Integer> countMap = new HashMap<>();
        if (dtoList == null || dtoList.isEmpty()) {
            return countMap;
        }

        for (CountFollowUnfollowMqDTO dto : dtoList) {
            FollowUnfollowTypeEnum typeEnum = FollowUnfollowTypeEnum.valueOf(dto.getType());
            // 非法 type：跳过
            if (Objects.isNull(typeEnum)) {
                continue;
            }

            int delta = switch (typeEnum) {
                case FOLLOW -> 1;   // 关注：粉丝数 +1
                case UNFOLLOW -> -1; // 取关：粉丝数 -1
            };
            countMap.merge(dto.getTargetUserId(), delta, Integer::sum);
        }
        return countMap;
    }
}
