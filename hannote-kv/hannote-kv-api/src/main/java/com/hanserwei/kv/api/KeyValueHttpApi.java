package com.hanserwei.kv.api;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.kv.api.dto.req.AddNoteContentReqDTO;
import com.hanserwei.kv.api.dto.req.BatchAddCommentContentReqDTO;
import com.hanserwei.kv.api.dto.req.BatchDeleteCommentContentReqDTO;
import com.hanserwei.kv.api.dto.req.BatchFindCommentContentReqDTO;
import com.hanserwei.kv.api.dto.req.DeleteNoteContentReqDTO;
import com.hanserwei.kv.api.dto.req.FindNoteContentReqDTO;
import com.hanserwei.kv.api.dto.resp.FindNoteContentRspDTO;
import com.hanserwei.kv.api.dto.resp.FindCommentContentRspDTO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * KV 键值存储服务对外契约（HTTP Interface）.
 *
 * <p>供其他服务通过 {@code @ImportHttpServices(group = KvApiConstants.SERVICE_NAME,
 * types = KeyValueHttpApi.class)} 声明并注入调用。这些接口仅供内网服务间 RPC 使用，
 * 用于存取笔记正文与评论正文内容，不经网关对外暴露。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@HttpExchange
public interface KeyValueHttpApi {

    /** KV 服务上下文前缀（与 NoteContentController 的 @RequestMapping 对齐） */
    String PREFIX = "/kv";

    /**
     * 新增笔记内容.
     *
     * @param addNoteContentReqDTO 新增入参
     * @return 操作结果
     */
    @PostExchange(PREFIX + "/note/content/add")
    Response<?> addNoteContent(@RequestBody AddNoteContentReqDTO addNoteContentReqDTO);

    /**
     * 根据 UUID 查询笔记内容.
     *
     * @param findNoteContentReqDTO 查询入参
     * @return 笔记内容
     */
    @PostExchange(PREFIX + "/note/content/find")
    Response<FindNoteContentRspDTO> findNoteContent(@RequestBody FindNoteContentReqDTO findNoteContentReqDTO);

    /**
     * 根据 UUID 删除笔记内容.
     *
     * @param deleteNoteContentReqDTO 删除入参
     * @return 操作结果
     */
    @PostExchange(PREFIX + "/note/content/delete")
    Response<?> deleteNoteContent(@RequestBody DeleteNoteContentReqDTO deleteNoteContentReqDTO);

    /**
     * 批量新增评论内容（写入 ScyllaDB comment_content）.
     *
     * @param batchAddCommentContentReqDTO 批量新增入参
     * @return 操作结果
     */
    @PostExchange(PREFIX + "/comment/content/batchAdd")
    Response<?> batchAddCommentContent(@RequestBody BatchAddCommentContentReqDTO batchAddCommentContentReqDTO);

    /**
     * 批量查询评论正文.
     *
     * @param request 批量查询入参
     * @return 正文集合，返回顺序不作承诺
     */
    @PostExchange(PREFIX + "/comment/content/batchFind")
    Response<List<FindCommentContentRspDTO>> batchFindCommentContent(
            @RequestBody BatchFindCommentContentReqDTO request);

    /**
     * 批量删除评论正文（幂等）.
     *
     * @param request 批量删除入参
     * @return 操作结果
     */
    @PostExchange(PREFIX + "/comment/content/batchDelete")
    Response<?> batchDeleteCommentContent(@RequestBody BatchDeleteCommentContentReqDTO request);
}
