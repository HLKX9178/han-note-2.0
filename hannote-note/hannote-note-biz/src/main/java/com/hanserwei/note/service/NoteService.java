package com.hanserwei.note.service;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.note.model.vo.FindNoteDetailReqVO;
import com.hanserwei.note.model.vo.FindNoteDetailRspVO;
import com.hanserwei.note.model.vo.PublishNoteReqVO;
import com.hanserwei.note.model.vo.UpdateNoteReqVO;

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
}
