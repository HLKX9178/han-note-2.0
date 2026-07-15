package com.hanserwei.comment.domain.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 评论自定义 Mapper XML 解析回归测试.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
class MapperXmlParseTest {

    @Test
    void PostgreSqlReturning与递归CTE映射可被MyBatis解析() {
        Configuration configuration = new Configuration();
        parse(configuration, "mapper/CommentDOMapper.xml");
        parse(configuration, "mapper/CommentLikeDOMapper.xml");

        assertThat(configuration.hasStatement(
                "com.hanserwei.comment.domain.mapper.CommentDOMapper.batchInsertReturning")).isTrue();
        assertThat(configuration.hasStatement(
                "com.hanserwei.comment.domain.mapper.CommentLikeDOMapper.batchInsertReturning")).isTrue();
    }

    private void parse(Configuration configuration, String resource) {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            assertThat(input).as(resource).isNotNull();
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        } catch (Exception e) {
            throw new AssertionError("Mapper XML 解析失败: " + resource, e);
        }
    }
}
