package com.hanserwei.comment.rpc;

import com.hanserwei.comment.enums.ResponseCodeEnum;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.note.api.NoteHttpApi;
import com.hanserwei.note.api.dto.req.FindPublishedNoteReqDTO;
import com.hanserwei.note.api.dto.resp.FindPublishedNoteRspDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 笔记服务 RPC 封装.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Service
@RequiredArgsConstructor
public class NoteRpcService {

    private final NoteHttpApi noteHttpApi;

    /**
     * 校验笔记处于正常发布状态.
     *
     * @param noteId 笔记 ID
     * @return 笔记最小信息
     */
    public FindPublishedNoteRspDTO requirePublished(Long noteId) {
        Response<FindPublishedNoteRspDTO> response = noteHttpApi.findPublishedById(
                FindPublishedNoteReqDTO.builder().noteId(noteId).build());
        if (Objects.isNull(response) || !response.isSuccess() || Objects.isNull(response.getData())) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }
        return response.getData();
    }
}
