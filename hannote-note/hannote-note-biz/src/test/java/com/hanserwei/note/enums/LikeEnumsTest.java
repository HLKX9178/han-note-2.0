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
    void bloomAddResult_valueOf() {
        assertEquals(NoteBloomAddResultEnum.NOT_EXIST, NoteBloomAddResultEnum.valueOf(-1L));
        assertEquals(NoteBloomAddResultEnum.ALREADY, NoteBloomAddResultEnum.valueOf(1L));
        assertEquals(NoteBloomAddResultEnum.SUCCESS, NoteBloomAddResultEnum.valueOf(0L));
        assertNull(NoteBloomAddResultEnum.valueOf(2L));
        assertNull(NoteBloomAddResultEnum.valueOf((Long) null));
    }

    @Test
    void bloomCheckResult_valueOf() {
        assertEquals(NoteBloomCheckResultEnum.NOT_EXIST, NoteBloomCheckResultEnum.valueOf(-1L));
        assertEquals(NoteBloomCheckResultEnum.MARKED, NoteBloomCheckResultEnum.valueOf(1L));
        assertEquals(NoteBloomCheckResultEnum.NOT_MARKED, NoteBloomCheckResultEnum.valueOf(0L));
        assertNull(NoteBloomCheckResultEnum.valueOf(2L));
        assertNull(NoteBloomCheckResultEnum.valueOf((Long) null));
    }

    @Test
    void collectUnCollectType_valueOf() {
        assertEquals(CollectUnCollectNoteTypeEnum.COLLECT, CollectUnCollectNoteTypeEnum.valueOf(1));
        assertEquals(CollectUnCollectNoteTypeEnum.UN_COLLECT, CollectUnCollectNoteTypeEnum.valueOf(0));
        assertNull(CollectUnCollectNoteTypeEnum.valueOf(9));
        assertNull(CollectUnCollectNoteTypeEnum.valueOf((Integer) null));
    }
}
