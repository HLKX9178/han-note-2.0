package com.hanserwei.note.service;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.note.model.vo.CollectNoteReqVO;
import com.hanserwei.note.model.vo.DeleteNoteReqVO;
import com.hanserwei.note.model.vo.FindNoteDetailReqVO;
import com.hanserwei.note.model.vo.FindNoteDetailRspVO;
import com.hanserwei.note.model.vo.LikeNoteReqVO;
import com.hanserwei.note.model.vo.PublishNoteReqVO;
import com.hanserwei.note.model.vo.TopNoteReqVO;
import com.hanserwei.note.model.vo.UnCollectNoteReqVO;
import com.hanserwei.note.model.vo.UnlikeNoteReqVO;
import com.hanserwei.note.model.vo.UpdateNoteReqVO;
import com.hanserwei.note.model.vo.UpdateNoteVisibleOnlyMeReqVO;

/**
 * 笔记业务.
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
public interface NoteService {

    /**
     * 笔记发布.
     *
     * @param publishNoteReqVO 发布入参
     * @return 操作结果
     */
    Response<?> publishNote(PublishNoteReqVO publishNoteReqVO);

    /**
     * 笔记详情（二级缓存：Caffeine L1 + Redis L2 + DB，并发调用下游服务）.
     *
     * @param findNoteDetailReqVO 查询入参
     * @return 笔记详情
     */
    Response<FindNoteDetailRspVO> findNoteDetail(FindNoteDetailReqVO findNoteDetailReqVO);

    /**
     * 笔记更新.
     *
     * @param updateNoteReqVO 更新入参
     * @return 操作结果
     */
    Response<?> updateNote(UpdateNoteReqVO updateNoteReqVO);

    /**
     * 删除笔记本地缓存（L1 Caffeine）.
     *
     * <p>供广播消息消费者调用，删除本实例进程内的笔记详情本地缓存。
     *
     * @param noteId 笔记 ID
     */
    void deleteNoteLocalCache(Long noteId);

    /**
     * 删除笔记（逻辑删除，status 置为 2）.
     *
     * @param deleteNoteReqVO 删除入参
     * @return 操作结果
     */
    Response<?> deleteNote(DeleteNoteReqVO deleteNoteReqVO);

    /**
     * 笔记仅对自己可见.
     *
     * @param updateNoteVisibleOnlyMeReqVO 入参
     * @return 操作结果
     */
    Response<?> visibleOnlyMe(UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO);

    /**
     * 笔记置顶 / 取消置顶.
     *
     * @param topNoteReqVO 入参
     * @return 操作结果
     */
    Response<?> topNote(TopNoteReqVO topNoteReqVO);

    /**
     * 点赞笔记（布隆过滤器 + ZSET + DB 三级判重，顺序 MQ 异步落库）.
     *
     * @param likeNoteReqVO 点赞入参
     * @return 操作结果
     */
    Response<?> likeNote(LikeNoteReqVO likeNoteReqVO);

    /**
     * 取消点赞笔记（布隆校验 + 删除 ZSET 成员，顺序 MQ 异步更新落库）.
     *
     * @param unlikeNoteReqVO 取消点赞入参
     * @return 操作结果
     */
    Response<?> unlikeNote(UnlikeNoteReqVO unlikeNoteReqVO);

    /**
     * 收藏笔记（布隆过滤器 + ZSET + DB 三级判重，顺序 MQ 异步落库）.
     *
     * @param collectNoteReqVO 收藏入参
     * @return 操作结果
     */
    Response<?> collectNote(CollectNoteReqVO collectNoteReqVO);

    /**
     * 取消收藏笔记（布隆校验 + 删除 ZSET 成员，顺序 MQ 异步更新落库）.
     *
     * @param unCollectNoteReqVO 取消收藏入参
     * @return 操作结果
     */
    Response<?> unCollectNote(UnCollectNoteReqVO unCollectNoteReqVO);
}
