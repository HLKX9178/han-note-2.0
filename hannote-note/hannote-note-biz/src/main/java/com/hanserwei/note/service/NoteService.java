package com.hanserwei.note.service;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.note.model.vo.PublishNoteReqVO;

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
}
