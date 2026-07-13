package com.hanserwei.search.service;

import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.search.model.vo.SearchNoteReqVO;
import com.hanserwei.search.model.vo.SearchNoteRspVO;

/**
 * 笔记搜索业务.
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
public interface NoteService {

    /**
     * 搜索笔记。
     *
     * @param searchNoteReqVO 搜索入参
     * @return 分页搜索结果
     */
    PageResponse<SearchNoteRspVO> searchNote(SearchNoteReqVO searchNoteReqVO);
}
