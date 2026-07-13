package com.hanserwei.search.controller;

import com.hanserwei.framework.biz.operationlog.aspect.ApiOperationLog;
import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.search.model.vo.SearchNoteReqVO;
import com.hanserwei.search.model.vo.SearchNoteRspVO;
import com.hanserwei.search.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 笔记搜索控制器.
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@RestController
@RequestMapping("/search")
@Slf4j
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping("/note")
    @ApiOperationLog(description = "搜索笔记")
    public PageResponse<SearchNoteRspVO> searchNote(@RequestBody @Valid SearchNoteReqVO searchNoteReqVO) {
        return noteService.searchNote(searchNoteReqVO);
    }
}
