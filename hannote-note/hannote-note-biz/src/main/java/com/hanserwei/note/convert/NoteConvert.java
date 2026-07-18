package com.hanserwei.note.convert;

import com.hanserwei.note.domain.dataobject.NoteDO;
import com.hanserwei.note.model.dto.PublishNoteDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 笔记实体转换（MapStruct）.
 *
 * @author hanserwei
 * @date 2026/07/18
 * @since 0.0.1
 */
@Mapper
public interface NoteConvert {

    /** 转换器实例 */
    NoteConvert INSTANCE = Mappers.getMapper(NoteConvert.class);

    /**
     * NoteDO 转 PublishNoteDTO（正文 content 需调用方另行 set）.
     *
     * @param bean 笔记 DO
     * @return 发布笔记 DTO
     */
    PublishNoteDTO convertDO2DTO(NoteDO bean);

    /**
     * PublishNoteDTO 转 NoteDO（content 非 DO 字段，忽略）.
     *
     * @param bean 发布笔记 DTO
     * @return 笔记 DO
     */
    NoteDO convertDTO2DO(PublishNoteDTO bean);
}
