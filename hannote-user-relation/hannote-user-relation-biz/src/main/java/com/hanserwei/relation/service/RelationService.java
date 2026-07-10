package com.hanserwei.relation.service;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.relation.model.vo.FollowUserReqVO;
import com.hanserwei.relation.model.vo.UnfollowUserReqVO;

/**
 * 用户关系业务.
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
public interface RelationService {

    /**
     * 关注用户.
     *
     * @param followUserReqVO 关注请求（含被关注用户 ID）
     * @return 统一响应
     */
    Response<?> follow(FollowUserReqVO followUserReqVO);

    /**
     * 取关用户.
     *
     * @param unfollowUserReqVO 取关请求（含被取关用户 ID）
     * @return 统一响应
     */
    Response<?> unfollow(UnfollowUserReqVO unfollowUserReqVO);
}
