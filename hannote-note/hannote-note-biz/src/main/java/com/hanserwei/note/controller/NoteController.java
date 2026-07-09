package com.hanserwei.note.controller;

import com.hanserwei.framework.biz.operationlog.aspect.ApiOperationLog;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.note.model.vo.DeleteNoteReqVO;
import com.hanserwei.note.model.vo.FindNoteDetailReqVO;
import com.hanserwei.note.model.vo.FindNoteDetailRspVO;
import com.hanserwei.note.model.vo.PublishNoteReqVO;
import com.hanserwei.note.model.vo.TopNoteReqVO;
import com.hanserwei.note.model.vo.UpdateNoteReqVO;
import com.hanserwei.note.model.vo.UpdateNoteVisibleOnlyMeReqVO;
import com.hanserwei.note.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 笔记控制器.
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Slf4j
@RestController
@RequestMapping("/note")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    /**
     * 笔记发布（图文 / 视频）.
     *
     * <p>经网关转发，需 JWT；发布者 ID 由网关透传的 userId 头解析得到。
     *
     * @param publishNoteReqVO 发布入参
     * @return 操作结果
     */
    @PostMapping("/publish")
    @ApiOperationLog(description = "笔记发布")
    public Response<?> publishNote(@Validated @RequestBody PublishNoteReqVO publishNoteReqVO) {
        return noteService.publishNote(publishNoteReqVO);
    }

    /**
     * 笔记详情（二级缓存 + 并发调用下游服务）.
     *
     * @param findNoteDetailReqVO 查询入参
     * @return 笔记详情
     */
    @PostMapping("/detail")
    @ApiOperationLog(description = "笔记详情")
    public Response<FindNoteDetailRspVO> findNoteDetail(@Validated @RequestBody FindNoteDetailReqVO findNoteDetailReqVO) {
        return noteService.findNoteDetail(findNoteDetailReqVO);
    }

    /**
     * 笔记更新（仅作者本人可改）.
     *
     * @param updateNoteReqVO 更新入参
     * @return 操作结果
     */
    @PostMapping("/update")
    @ApiOperationLog(description = "笔记修改")
    public Response<?> updateNote(@Validated @RequestBody UpdateNoteReqVO updateNoteReqVO) {
        return noteService.updateNote(updateNoteReqVO);
    }

    /**
     * 笔记删除（逻辑删除，仅作者本人可删）.
     *
     * @param deleteNoteReqVO 删除入参
     * @return 操作结果
     */
    @PostMapping("/delete")
    @ApiOperationLog(description = "删除笔记")
    public Response<?> deleteNote(@Validated @RequestBody DeleteNoteReqVO deleteNoteReqVO) {
        return noteService.deleteNote(deleteNoteReqVO);
    }

    /**
     * 笔记仅对自己可见（仅作者本人可改）.
     *
     * @param updateNoteVisibleOnlyMeReqVO 入参
     * @return 操作结果
     */
    @PostMapping("/visible/onlyme")
    @ApiOperationLog(description = "笔记仅对自己可见")
    public Response<?> visibleOnlyMe(@Validated @RequestBody UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO) {
        return noteService.visibleOnlyMe(updateNoteVisibleOnlyMeReqVO);
    }

    /**
     * 笔记置顶 / 取消置顶（仅作者本人可操作）.
     *
     * @param topNoteReqVO 入参
     * @return 操作结果
     */
    @PostMapping("/top")
    @ApiOperationLog(description = "置顶/取消置顶笔记")
    public Response<?> topNote(@Validated @RequestBody TopNoteReqVO topNoteReqVO) {
        return noteService.topNote(topNoteReqVO);
    }
}
