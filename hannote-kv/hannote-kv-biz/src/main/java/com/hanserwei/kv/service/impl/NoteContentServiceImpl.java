package com.hanserwei.kv.service.impl;

import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.kv.api.dto.req.AddNoteContentReqDTO;
import com.hanserwei.kv.api.dto.req.DeleteNoteContentReqDTO;
import com.hanserwei.kv.api.dto.req.FindNoteContentReqDTO;
import com.hanserwei.kv.api.dto.resp.FindNoteContentRspDTO;
import com.hanserwei.kv.domain.dataobject.NoteContentDO;
import com.hanserwei.kv.domain.repository.NoteContentRepository;
import com.hanserwei.kv.enums.ResponseCodeEnum;
import com.hanserwei.kv.service.NoteContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * 笔记内容存储业务实现（底层对接 ScyllaDB）.
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NoteContentServiceImpl implements NoteContentService {

    private final NoteContentRepository noteContentRepository;

    @Override
    public Response<?> addNoteContent(AddNoteContentReqDTO addNoteContentReqDTO) {
        // 笔记内容 UUID（由笔记服务生成并传入）
        String uuid = addNoteContentReqDTO.getUuid();
        // 笔记内容
        String content = addNoteContentReqDTO.getContent();

        // 构建数据库 DO 实体类（以笔记服务传入的 UUID 作主键）
        NoteContentDO noteContentDO = NoteContentDO.builder()
                .id(UUID.fromString(uuid))
                .content(content)
                .build();

        // 插入数据
        noteContentRepository.save(noteContentDO);
        log.info("==> 笔记内容已保存, uuid: {}", noteContentDO.getId());

        return Response.success();
    }

    @Override
    public Response<FindNoteContentRspDTO> findNoteContent(FindNoteContentReqDTO findNoteContentReqDTO) {
        // 笔记内容 UUID
        String uuid = findNoteContentReqDTO.getUuid();

        // 根据 UUID 查询笔记内容
        Optional<NoteContentDO> optional = noteContentRepository.findById(UUID.fromString(uuid));

        // 若笔记内容不存在，抛出业务异常交由全局异常处理器统一处理
        if (optional.isEmpty()) {
            throw new BizException(ResponseCodeEnum.NOTE_CONTENT_NOT_FOUND);
        }

        NoteContentDO noteContentDO = optional.get();
        // 构建返参 DTO
        FindNoteContentRspDTO findNoteContentRspDTO = FindNoteContentRspDTO.builder()
                .uuid(noteContentDO.getId())
                .content(noteContentDO.getContent())
                .build();

        return Response.success(findNoteContentRspDTO);
    }

    @Override
    public Response<?> deleteNoteContent(DeleteNoteContentReqDTO deleteNoteContentReqDTO) {
        // 笔记内容 UUID
        String uuid = deleteNoteContentReqDTO.getUuid();

        // 根据主键删除笔记内容
        noteContentRepository.deleteById(UUID.fromString(uuid));
        log.info("==> 笔记内容已删除, uuid: {}", uuid);

        return Response.success();
    }
}
