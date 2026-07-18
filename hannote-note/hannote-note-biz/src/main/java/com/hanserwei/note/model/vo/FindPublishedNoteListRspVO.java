package com.hanserwei.note.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 个人主页 - 已发布笔记列表响应.
 *
 * @author hanserwei
 * @date 2026/07/18
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindPublishedNoteListRspVO {

    /** 笔记分页数据 */
    private List<NoteItemRspVO> notes;

    /** 下一页的游标（本页最小笔记 ID） */
    private Long nextCursor;
}
