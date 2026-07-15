package com.hanserwei.comment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanserwei.comment.cache.CommentCacheManager;
import com.hanserwei.comment.config.CommentQueryExecutor;
import com.hanserwei.comment.constant.MQConstants;
import com.hanserwei.comment.constant.RedisKeyConstants;
import com.hanserwei.comment.domain.dataobject.CommentDO;
import com.hanserwei.comment.domain.dataobject.CommentLikeDO;
import com.hanserwei.comment.domain.mapper.CommentDOMapper;
import com.hanserwei.comment.domain.mapper.CommentLikeDOMapper;
import com.hanserwei.comment.enums.CommentLikeUnlikeTypeEnum;
import com.hanserwei.comment.enums.ResponseCodeEnum;
import com.hanserwei.comment.model.dto.LikeUnlikeCommentMqDTO;
import com.hanserwei.comment.model.vo.LikeCommentReqVO;
import com.hanserwei.comment.retry.SendMqRetryHelper;
import com.hanserwei.comment.service.CommentInteractionService;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 评论点赞/取消点赞业务实现（RedisBloom + DB 判重 + 可靠顺序 MQ）.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentInteractionServiceImpl implements CommentInteractionService {

    private static final DefaultRedisScript<Long> BLOOM_CHECK_AND_ADD = script(
            "/lua/comment_bloom_check_and_add.lua");
    private static final DefaultRedisScript<Long> BLOOM_EXIST = script(
            "/lua/comment_bloom_exist.lua");
    private static final DefaultRedisScript<Long> BLOOM_BATCH_ADD = script(
            "/lua/comment_bloom_batch_add_and_expire.lua");

    private final CommentDOMapper commentDOMapper;
    private final CommentLikeDOMapper commentLikeDOMapper;
    private final CommentCacheManager commentCacheManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final SendMqRetryHelper sendMqRetryHelper;
    private final CommentQueryExecutor commentQueryExecutor;

    @Override
    public Response<?> like(LikeCommentReqVO request) {
        requireComment(request.getCommentId());
        Long userId = LoginUserContextHolder.getUserId();
        String bloomKey = RedisKeyConstants.buildLikeBloomKey(userId);
        Long result = stringRedisTemplate.execute(BLOOM_CHECK_AND_ADD,
                Collections.singletonList(bloomKey), String.valueOf(request.getCommentId()));

        if (Objects.equals(result, -1L)) {
            if (isLiked(userId, request.getCommentId())) {
                commentQueryExecutor.execute(() -> initializeBloom(userId, bloomKey, null));
                throw new BizException(ResponseCodeEnum.COMMENT_ALREADY_LIKED);
            }
            initializeBloom(userId, bloomKey, request.getCommentId());
        } else if (Objects.equals(result, 1L) && isLiked(userId, request.getCommentId())) {
            throw new BizException(ResponseCodeEnum.COMMENT_ALREADY_LIKED);
        } else if (!Objects.equals(result, 0L) && !Objects.equals(result, 1L)) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        sendInteraction(userId, request.getCommentId(), CommentLikeUnlikeTypeEnum.LIKE);
        return Response.success();
    }

    @Override
    public Response<?> unlike(LikeCommentReqVO request) {
        requireComment(request.getCommentId());
        Long userId = LoginUserContextHolder.getUserId();
        String bloomKey = RedisKeyConstants.buildLikeBloomKey(userId);
        Long result = stringRedisTemplate.execute(BLOOM_EXIST,
                Collections.singletonList(bloomKey), String.valueOf(request.getCommentId()));

        if (Objects.equals(result, -1L)) {
            boolean liked = isLiked(userId, request.getCommentId());
            commentQueryExecutor.execute(() -> initializeBloom(userId, bloomKey, null));
            if (!liked) {
                throw new BizException(ResponseCodeEnum.COMMENT_NOT_LIKED);
            }
        } else if (Objects.equals(result, 0L)) {
            throw new BizException(ResponseCodeEnum.COMMENT_NOT_LIKED);
        } else if (!Objects.equals(result, 1L)) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        sendInteraction(userId, request.getCommentId(), CommentLikeUnlikeTypeEnum.UNLIKE);
        return Response.success();
    }

    private void requireComment(Long commentId) {
        if (commentCacheManager.getLocalDetail(commentId) != null) {
            return;
        }
        String cached = stringRedisTemplate.opsForValue().get(RedisKeyConstants.buildDetailKey(commentId));
        if (cached != null) {
            if ("null".equals(cached)) {
                throw new BizException(ResponseCodeEnum.COMMENT_NOT_FOUND);
            }
            return;
        }
        CommentDO comment = commentDOMapper.selectById(commentId);
        if (comment == null) {
            stringRedisTemplate.opsForValue().set(RedisKeyConstants.buildDetailKey(commentId), "null",
                    Duration.ofSeconds(60 + ThreadLocalRandom.current().nextInt(61)));
            throw new BizException(ResponseCodeEnum.COMMENT_NOT_FOUND);
        }
    }

    private boolean isLiked(Long userId, Long commentId) {
        return commentLikeDOMapper.selectCount(new LambdaQueryWrapper<CommentLikeDO>()
                .eq(CommentLikeDO::getUserId, userId)
                .eq(CommentLikeDO::getCommentId, commentId)) > 0;
    }

    private void initializeBloom(Long userId, String bloomKey, Long currentCommentId) {
        try {
            List<CommentLikeDO> likes = commentLikeDOMapper.selectList(new LambdaQueryWrapper<CommentLikeDO>()
                    .select(CommentLikeDO::getCommentId)
                    .eq(CommentLikeDO::getUserId, userId));
            List<String> args = new ArrayList<>();
            likes.stream().map(CommentLikeDO::getCommentId).map(String::valueOf).forEach(args::add);
            if (currentCommentId != null && likes.stream()
                    .noneMatch(item -> Objects.equals(item.getCommentId(), currentCommentId))) {
                args.add(String.valueOf(currentCommentId));
            }
            args.add(String.valueOf(randomBloomTtl()));
            stringRedisTemplate.execute(BLOOM_BATCH_ADD, Collections.singletonList(bloomKey), args.toArray());
        } catch (Exception e) {
            log.error("==> 初始化评论点赞 Bloom 失败, userId: {}", userId, e);
        }
    }

    private void sendInteraction(Long userId, Long commentId, CommentLikeUnlikeTypeEnum type) {
        LikeUnlikeCommentMqDTO dto = LikeUnlikeCommentMqDTO.builder()
                .userId(userId)
                .commentId(commentId)
                .type(type.getCode())
                .createTime(LocalDateTime.now())
                .build();
        String tag = type == CommentLikeUnlikeTypeEnum.LIKE ? MQConstants.TAG_LIKE : MQConstants.TAG_UNLIKE;
        sendMqRetryHelper.asyncSendOrderly(MQConstants.TOPIC_LIKE_UNLIKE_COMMENT + ":" + tag,
                JsonUtils.toJsonString(dto), String.valueOf(userId));
    }

    private long randomBloomTtl() {
        return Duration.ofDays(1).toSeconds()
                + ThreadLocalRandom.current().nextLong(Duration.ofDays(1).toSeconds() + 1);
    }

    private static DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        script.setResultType(Long.class);
        return script;
    }
}
