package com.hanserwei.note.controller;

import com.hanserwei.framework.biz.operationlog.aspect.ApiOperationLog;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.note.api.dto.req.FindPublishedNoteReqDTO;
import com.hanserwei.note.api.dto.resp.FindPublishedNoteRspDTO;
import com.hanserwei.note.model.vo.CollectNoteReqVO;
import com.hanserwei.note.model.vo.DeleteNoteReqVO;
import com.hanserwei.note.model.vo.FindNoteDetailReqVO;
import com.hanserwei.note.model.vo.FindNoteDetailRspVO;
import com.hanserwei.note.model.vo.FindNoteIsLikedAndCollectedReqVO;
import com.hanserwei.note.model.vo.FindNoteIsLikedAndCollectedRspVO;
import com.hanserwei.note.model.vo.FindPublishedNoteListReqVO;
import com.hanserwei.note.model.vo.FindPublishedNoteListRspVO;
import com.hanserwei.note.model.vo.LikeNoteReqVO;
import com.hanserwei.note.model.vo.PublishNoteReqVO;
import com.hanserwei.note.model.vo.TopNoteReqVO;
import com.hanserwei.note.model.vo.UnCollectNoteReqVO;
import com.hanserwei.note.model.vo.UnlikeNoteReqVO;
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
     * 查询正常发布中的笔记（内网 RPC）.
     */
    @PostMapping("/findPublishedById")
    public Response<FindPublishedNoteRspDTO> findPublishedById(
            @Validated @RequestBody FindPublishedNoteReqDTO request) {
        return noteService.findPublishedById(request);
    }

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

    /**
     * 点赞笔记.
     *
     * @param likeNoteReqVO 点赞入参
     * @return 操作结果
     */
    @PostMapping("/like")
    @ApiOperationLog(description = "点赞笔记")
    public Response<?> likeNote(@Validated @RequestBody LikeNoteReqVO likeNoteReqVO) {
        return noteService.likeNote(likeNoteReqVO);
    }

    /**
     * 取消点赞笔记.
     *
     * @param unlikeNoteReqVO 取消点赞入参
     * @return 操作结果
     */
    @PostMapping("/unlike")
    @ApiOperationLog(description = "取消点赞笔记")
    public Response<?> unlikeNote(@Validated @RequestBody UnlikeNoteReqVO unlikeNoteReqVO) {
        return noteService.unlikeNote(unlikeNoteReqVO);
    }

    /**
     * 收藏笔记.
     *
     * @param collectNoteReqVO 收藏入参
     * @return 操作结果
     */
    @PostMapping("/collect")
    @ApiOperationLog(description = "收藏笔记")
    public Response<?> collectNote(@Validated @RequestBody CollectNoteReqVO collectNoteReqVO) {
        return noteService.collectNote(collectNoteReqVO);
    }

    /**
     * 取消收藏笔记.
     *
     * @param unCollectNoteReqVO 取消收藏入参
     * @return 操作结果
     */
    @PostMapping("/uncollect")
    @ApiOperationLog(description = "取消收藏笔记")
    public Response<?> unCollectNote(@Validated @RequestBody UnCollectNoteReqVO unCollectNoteReqVO) {
        return noteService.unCollectNote(unCollectNoteReqVO);
    }

    /**
     * 个人主页 - 已发布笔记列表（游标分页）.
     *
     * <p>经网关白名单放行，匿名可查看他人主页；登录后返回每篇笔记的 isLiked，
     * 博主本人查看时点赞量实时。
     *
     * @param findPublishedNoteListReqVO 查询入参（目标博主 userId + 游标 cursor）
     * @return 笔记卡片列表 + 下一页游标
     */
    @PostMapping("/published/list")
    @ApiOperationLog(description = "个人主页-已发布笔记列表")
    public Response<FindPublishedNoteListRspVO> findPublishedNoteList(
            @Validated @RequestBody FindPublishedNoteListReqVO findPublishedNoteListReqVO) {
        return noteService.findPublishedNoteList(findPublishedNoteListReqVO);
    }

    /**
     * 获取当前登录用户对某笔记的「是否点赞、是否收藏」数据.
     *
     * @param findNoteIsLikedAndCollectedReqVO 入参（笔记 ID）
     * @return 点赞 / 收藏标识
     */
    @PostMapping("/isLikedAndCollectedData")
    @ApiOperationLog(description = "获取当前用户是否点赞、收藏数据")
    public Response<FindNoteIsLikedAndCollectedRspVO> isLikedAndCollectedData(
            @Validated @RequestBody FindNoteIsLikedAndCollectedReqVO findNoteIsLikedAndCollectedReqVO) {
        return noteService.isLikedAndCollectedData(findNoteIsLikedAndCollectedReqVO);
    }
}
