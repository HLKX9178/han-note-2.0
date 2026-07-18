package com.hanserwei.note.rpc;

import com.hanserwei.count.api.CountHttpApi;
import com.hanserwei.count.api.dto.req.FindNoteCountsByIdsReqDTO;
import com.hanserwei.count.api.dto.resp.FindNoteCountRspDTO;
import com.hanserwei.framework.common.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 计数服务调用封装.
 *
 * <p>通过 {@link CountHttpApi} 批量查询笔记计数（点赞/收藏/评论），供已发布笔记列表展示点赞量。
 *
 * @author hanserwei
 * @date 2026/07/18
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CountRpcService {

    private final CountHttpApi countHttpApi;

    /**
     * 根据笔记 ID 集合批量查询笔记计数.
     *
     * @param noteIds 笔记 ID 集合
     * @return 各笔记的计数集合；失败或空返回 {@code null}
     */
    public List<FindNoteCountRspDTO> findByNoteIds(List<Long> noteIds) {
        FindNoteCountsByIdsReqDTO request = FindNoteCountsByIdsReqDTO.builder()
                .noteIds(noteIds)
                .build();

        Response<List<FindNoteCountRspDTO>> response = countHttpApi.findNotesCountData(request);
        if (Objects.isNull(response) || !response.isSuccess() || Objects.isNull(response.getData())) {
            log.error("==> 调用计数服务批量查询笔记计数失败, noteIds: {}, response: {}", noteIds, response);
            return null;
        }
        return response.getData();
    }
}
