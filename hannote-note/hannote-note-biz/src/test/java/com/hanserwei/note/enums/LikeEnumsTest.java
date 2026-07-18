package com.hanserwei.note.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 点赞相关枚举反查逻辑单元测试（纯逻辑，不依赖中间件）.
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
class LikeEnumsTest {

    @Test
    void likeUnlikeType_valueOf() {
        assertEquals(LikeUnlikeNoteTypeEnum.LIKE, LikeUnlikeNoteTypeEnum.valueOf(1));
        assertEquals(LikeUnlikeNoteTypeEnum.UNLIKE, LikeUnlikeNoteTypeEnum.valueOf(0));
        assertNull(LikeUnlikeNoteTypeEnum.valueOf(9));
        assertNull(LikeUnlikeNoteTypeEnum.valueOf((Integer) null));
    }

    @Test
    void rbitmapAddResult_valueOf() {
        assertEquals(NoteRBitmapAddResultEnum.NOT_EXIST, NoteRBitmapAddResultEnum.valueOf(-1L));
        assertEquals(NoteRBitmapAddResultEnum.ALREADY, NoteRBitmapAddResultEnum.valueOf(1L));
        assertEquals(NoteRBitmapAddResultEnum.SUCCESS, NoteRBitmapAddResultEnum.valueOf(0L));
        assertNull(NoteRBitmapAddResultEnum.valueOf(2L));
        assertNull(NoteRBitmapAddResultEnum.valueOf((Long) null));
    }

    @Test
    void rbitmapCheckResult_valueOf() {
        assertEquals(NoteRBitmapCheckResultEnum.NOT_EXIST, NoteRBitmapCheckResultEnum.valueOf(-1L));
        assertEquals(NoteRBitmapCheckResultEnum.MARKED, NoteRBitmapCheckResultEnum.valueOf(1L));
        assertEquals(NoteRBitmapCheckResultEnum.NOT_MARKED, NoteRBitmapCheckResultEnum.valueOf(0L));
        assertNull(NoteRBitmapCheckResultEnum.valueOf(2L));
        assertNull(NoteRBitmapCheckResultEnum.valueOf((Long) null));
    }

    @Test
    void collectUnCollectType_valueOf() {
        assertEquals(CollectUnCollectNoteTypeEnum.COLLECT, CollectUnCollectNoteTypeEnum.valueOf(1));
        assertEquals(CollectUnCollectNoteTypeEnum.UN_COLLECT, CollectUnCollectNoteTypeEnum.valueOf(0));
        assertNull(CollectUnCollectNoteTypeEnum.valueOf(9));
        assertNull(CollectUnCollectNoteTypeEnum.valueOf((Integer) null));
    }
}
