package com.hanserwei.note.rpc;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.kv.api.KeyValueHttpApi;
import com.hanserwei.kv.api.dto.req.AddNoteContentReqDTO;
import com.hanserwei.kv.api.dto.req.DeleteNoteContentReqDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * KV 键值服务调用封装.
 *
 * <p>通过 {@link KeyValueHttpApi} 存取笔记正文内容（底层为 ScyllaDB）。
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeyValueRpcService {

    private final KeyValueHttpApi keyValueHttpApi;

    /**
     * 保存笔记内容.
     *
     * @param uuid    笔记内容 UUID
     * @param content 笔记正文
     * @return 保存成功返回 {@code true}
     */
    public boolean saveNoteContent(String uuid, String content) {
        AddNoteContentReqDTO addNoteContentReqDTO = AddNoteContentReqDTO.builder()
                .uuid(uuid)
                .content(content)
                .build();

        Response<?> response = keyValueHttpApi.addNoteContent(addNoteContentReqDTO);
        if (Objects.isNull(response) || !response.isSuccess()) {
            log.error("==> 调用 KV 服务保存笔记内容失败, uuid: {}, response: {}", uuid, response);
            return false;
        }
        return true;
    }

    /**
     * 删除笔记内容（笔记元数据入库失败时的补偿操作）.
     *
     * @param uuid 笔记内容 UUID
     * @return 删除成功返回 {@code true}
     */
    public boolean deleteNoteContent(String uuid) {
        DeleteNoteContentReqDTO deleteNoteContentReqDTO = DeleteNoteContentReqDTO.builder()
                .uuid(uuid)
                .build();

        Response<?> response = keyValueHttpApi.deleteNoteContent(deleteNoteContentReqDTO);
        if (Objects.isNull(response) || !response.isSuccess()) {
            log.error("==> 调用 KV 服务删除笔记内容失败, uuid: {}, response: {}", uuid, response);
            return false;
        }
        return true;
    }
}
