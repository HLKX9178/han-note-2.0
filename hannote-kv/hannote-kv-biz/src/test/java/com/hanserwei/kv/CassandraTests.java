package com.hanserwei.kv;

import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.kv.domain.dataobject.NoteContentDO;
import com.hanserwei.kv.domain.repository.NoteContentRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ScyllaDB（Cassandra 协议）CRUD 集成测试.
 *
 * <p>验证 {@link NoteContentRepository} 对 {@code note_content} 表的增删改查。
 * 需 Scylla 实例与 Nacos 就绪；同一条记录在单个用例内完成 保存 → 查询 → 更新 → 删除 → 校验删除，
 * 用例自清理、不残留数据。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@SpringBootTest
@Slf4j
class CassandraTests {

    @Autowired
    private NoteContentRepository noteContentRepository;

    /**
     * 全流程：插入 → 查询 → 更新 → 删除.
     */
    @Test
    void crud() {
        UUID id = UUID.randomUUID();

        // 1. 插入
        noteContentRepository.save(NoteContentDO.builder()
                .id(id)
                .content("代码测试笔记内容插入")
                .build());

        // 2. 查询，确认插入成功
        Optional<NoteContentDO> inserted = noteContentRepository.findById(id);
        assertThat(inserted).isPresent();
        assertThat(inserted.get().getContent()).isEqualTo("代码测试笔记内容插入");
        log.info("==> 插入后查询结果：{}", JsonUtils.toJsonString(inserted.get()));

        // 3. 更新（主键一致即为更新）
        noteContentRepository.save(NoteContentDO.builder()
                .id(id)
                .content("代码测试笔记内容更新")
                .build());
        Optional<NoteContentDO> updated = noteContentRepository.findById(id);
        assertThat(updated).isPresent();
        assertThat(updated.get().getContent()).isEqualTo("代码测试笔记内容更新");
        log.info("==> 更新后查询结果：{}", JsonUtils.toJsonString(updated.get()));

        // 4. 删除，确认删除成功
        noteContentRepository.deleteById(id);
        assertThat(noteContentRepository.findById(id)).isEmpty();
        log.info("==> 已删除 id: {}", id);
    }
}
