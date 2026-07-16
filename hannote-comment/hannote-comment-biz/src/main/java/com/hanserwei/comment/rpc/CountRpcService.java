package com.hanserwei.comment.rpc;

import com.hanserwei.count.api.CountHttpApi;
import com.hanserwei.count.api.dto.req.FindNoteCountReqDTO;
import com.hanserwei.count.api.dto.resp.FindNoteCountRspDTO;
import com.hanserwei.framework.common.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 计数服务 RPC 封装.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CountRpcService {

    private final CountHttpApi countHttpApi;

    /**
     * 查询笔记全部评论数；失败返回 fallback.
     *
     * @param noteId   笔记 ID
     * @param fallback 计数服务不可用时的兜底值（一般为一级评论总数）
     * @return 计数服务返回值与 fallback 的较大者；失败降级为 fallback
     */
    public long findCommentTotal(Long noteId, long fallback) {
        try {
            Response<FindNoteCountRspDTO> response = countHttpApi.findNoteCountById(
                    FindNoteCountReqDTO.builder().noteId(noteId).build());
            if (response != null && response.isSuccess() && response.getData() != null
                    && response.getData().getCommentTotal() != null) {
                return Math.max(fallback, response.getData().getCommentTotal());
            }
        } catch (Exception e) {
            log.error("==> 查询笔记评论总数失败，降级为一级评论总数, noteId: {}", noteId, e);
        }
        return fallback;
    }
}
