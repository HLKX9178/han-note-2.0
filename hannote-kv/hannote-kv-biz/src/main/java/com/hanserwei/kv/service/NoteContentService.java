package com.hanserwei.kv.service;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.kv.api.dto.req.AddNoteContentReqDTO;
import com.hanserwei.kv.api.dto.req.DeleteNoteContentReqDTO;
import com.hanserwei.kv.api.dto.req.FindNoteContentReqDTO;
import com.hanserwei.kv.api.dto.resp.FindNoteContentRspDTO;

/**
 * 笔记内容存储业务.
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
public interface NoteContentService {

    /**
     * 新增笔记内容.
     *
     * @param addNoteContentReqDTO 新增入参
     * @return 操作结果
     */
    Response<?> addNoteContent(AddNoteContentReqDTO addNoteContentReqDTO);

    /**
     * 根据笔记 ID 查询笔记内容.
     *
     * @param findNoteContentReqDTO 查询入参
     * @return 笔记内容
     */
    Response<FindNoteContentRspDTO> findNoteContent(FindNoteContentReqDTO findNoteContentReqDTO);

    /**
     * 根据笔记 ID 删除笔记内容.
     *
     * @param deleteNoteContentReqDTO 删除入参
     * @return 操作结果
     */
    Response<?> deleteNoteContent(DeleteNoteContentReqDTO deleteNoteContentReqDTO);
}
