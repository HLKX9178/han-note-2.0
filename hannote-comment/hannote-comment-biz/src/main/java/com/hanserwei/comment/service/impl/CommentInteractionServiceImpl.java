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

    /** 布隆过滤器判存并原子写入：返回 -1 表示 key 不存在需初始化，1 已存在，0 新写入 */
    private static final DefaultRedisScript<Long> BLOOM_CHECK_AND_ADD = script(
            "/lua/comment_bloom_check_and_add.lua");
    /** 布隆过滤器仅判存（取消点赞用）：返回 -1 需初始化，1 存在，0 不存在 */
    private static final DefaultRedisScript<Long> BLOOM_EXIST = script(
            "/lua/comment_bloom_exist.lua");
    /** 布隆过滤器批量写入并设过期：初始化时全量回灌用户已点赞评论 */
    private static final DefaultRedisScript<Long> BLOOM_BATCH_ADD = script(
            "/lua/comment_bloom_batch_add_and_expire.lua");

    private final CommentDOMapper commentDOMapper;
    private final CommentLikeDOMapper commentLikeDOMapper;
    /** 评论本地缓存管理，用于快速判存评论 */
    private final CommentCacheManager commentCacheManager;
    private final StringRedisTemplate stringRedisTemplate;
    /** MQ 可靠发送助手：异步顺序发送点赞/取消点赞消息 */
    private final SendMqRetryHelper sendMqRetryHelper;
    /** 查询异步执行器（虚拟线程），用于异步初始化布隆过滤器 */
    private final CommentQueryExecutor commentQueryExecutor;

    /**
     * 评论点赞.
     *
     * <p>先校验评论存在，再走布隆过滤器 + DB 兜底判重：
     * Lua 脚本原子判存并写入布隆过滤器，返回 -1 表示 key 尚未初始化，
     * 需先回源 DB 判断是否已点赞（已赞则异步初始化布隆并报错），否则同步初始化布隆并放行；
     * 返回 1（可能存在，布隆有假阳性）时再查 DB 确认是否重复点赞。
     * 判重通过后经 MQ 异步落库计数（顺序消息保证同用户操作有序）。
     *
     * @param request 点赞请求（评论 ID）
     * @return 统一成功响应
     * @throws BizException 评论不存在、已点赞或系统异常时抛出
     */
    @Override
    public Response<?> like(LikeCommentReqVO request) {
        requireComment(request.getCommentId());
        Long userId = LoginUserContextHolder.getUserId();
        String bloomKey = RedisKeyConstants.buildLikeBloomKey(userId);
        // 原子判存并写入布隆过滤器
        Long result = stringRedisTemplate.execute(BLOOM_CHECK_AND_ADD,
                Collections.singletonList(bloomKey), String.valueOf(request.getCommentId()));

        if (Objects.equals(result, -1L)) {
            // 布隆未初始化：回源 DB 判重
            if (isLiked(userId, request.getCommentId())) {
                commentQueryExecutor.execute(() -> initializeBloom(userId, bloomKey, null));
                throw new BizException(ResponseCodeEnum.COMMENT_ALREADY_LIKED);
            }
            initializeBloom(userId, bloomKey, request.getCommentId());
        } else if (Objects.equals(result, 1L) && isLiked(userId, request.getCommentId())) {
            // 布隆判为可能存在（有假阳性），回源 DB 确认确实已点赞
            throw new BizException(ResponseCodeEnum.COMMENT_ALREADY_LIKED);
        } else if (!Objects.equals(result, 0L) && !Objects.equals(result, 1L)) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        // 判重通过：经 MQ 异步顺序落库计数
        sendInteraction(userId, request.getCommentId(), CommentLikeUnlikeTypeEnum.LIKE);
        return Response.success();
    }

    /**
     * 取消评论点赞.
     *
     * <p>校验评论存在后走布隆判存 + DB 兜底：Lua 返回 -1 表示布隆未初始化，
     * 回源 DB 判断是否点赞过并异步重建布隆；返回 0（布隆无假阴性，判不存在即真不存在）直接报未点赞；
     * 判存通过后经 MQ 异步落库扣减计数。
     *
     * @param request 取消点赞请求（评论 ID）
     * @return 统一成功响应
     * @throws BizException 评论不存在、未点赞或系统异常时抛出
     */
    @Override
    public Response<?> unlike(LikeCommentReqVO request) {
        requireComment(request.getCommentId());
        Long userId = LoginUserContextHolder.getUserId();
        String bloomKey = RedisKeyConstants.buildLikeBloomKey(userId);
        // 仅判存，不写入
        Long result = stringRedisTemplate.execute(BLOOM_EXIST,
                Collections.singletonList(bloomKey), String.valueOf(request.getCommentId()));

        if (Objects.equals(result, -1L)) {
            // 布隆未初始化：回源 DB 判存并异步重建布隆
            boolean liked = isLiked(userId, request.getCommentId());
            commentQueryExecutor.execute(() -> initializeBloom(userId, bloomKey, null));
            if (!liked) {
                throw new BizException(ResponseCodeEnum.COMMENT_NOT_LIKED);
            }
        } else if (Objects.equals(result, 0L)) {
            // 布隆无假阴性：判不存在即确未点赞
            throw new BizException(ResponseCodeEnum.COMMENT_NOT_LIKED);
        } else if (!Objects.equals(result, 1L)) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        // 判存通过：经 MQ 异步顺序落库扣减计数
        sendInteraction(userId, request.getCommentId(), CommentLikeUnlikeTypeEnum.UNLIKE);
        return Response.success();
    }

    /**
     * 校验评论存在（本地缓存 → Redis 详情/哨兵 → DB），不存在则写空哨兵并抛异常.
     *
     * @param commentId 评论 ID
     * @throws BizException 评论不存在时抛出
     */
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

    /**
     * 回源 DB 判断用户是否已点赞该评论（布隆假阳性兜底）.
     *
     * @param userId    用户 ID
     * @param commentId 评论 ID
     * @return 已点赞返回 true
     */
    private boolean isLiked(Long userId, Long commentId) {
        return commentLikeDOMapper.selectCount(new LambdaQueryWrapper<CommentLikeDO>()
                .eq(CommentLikeDO::getUserId, userId)
                .eq(CommentLikeDO::getCommentId, commentId)) > 0;
    }

    /**
     * 初始化用户点赞布隆过滤器：全量回灌该用户已点赞的评论 ID 并设随机 TTL.
     *
     * <p>可选叠加当前正在点赞的评论 ID（若尚不在已点赞集合中），一次写入避免竞态遗漏。
     * 内部吞异常并告警（不阻断主流程），下次仍会因 key 缺失重新初始化。
     *
     * @param userId           用户 ID
     * @param bloomKey         布隆过滤器 Redis key
     * @param currentCommentId 当前正在操作的评论 ID，可为 null（仅回灌历史）
     */
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

    /**
     * 经 MQ 异步顺序发送点赞/取消点赞消息（以 userId 为分区键，保证同用户操作有序）.
     *
     * @param userId    用户 ID
     * @param commentId 评论 ID
     * @param type      操作类型（点赞 / 取消点赞）
     */
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

    /**
     * 生成布隆过滤器随机 TTL（1~2 天），打散过期时间防雪崩.
     *
     * @return 随机 TTL 秒数
     */
    private long randomBloomTtl() {
        return Duration.ofDays(1).toSeconds()
                + ThreadLocalRandom.current().nextLong(Duration.ofDays(1).toSeconds() + 1);
    }

    /**
     * 从类路径加载 Lua 脚本资源，构建返回 Long 的 Redis 脚本对象.
     *
     * @param path Lua 脚本类路径
     * @return Redis 脚本对象
     */
    private static DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        script.setResultType(Long.class);
        return script;
    }
}
