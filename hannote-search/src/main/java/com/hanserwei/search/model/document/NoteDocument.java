package com.hanserwei.search.model.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记索引文档.
 *
 * <p>ES {@code note} 索引命中文档的反序列化载体。发布者字段为 {@code creator_nickname}/
 * {@code creator_avatar}；时间字段为 {@code yyyy-MM-dd HH:mm:ss} 字符串，业务层再转换。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoteDocument {

    private Long id;

    private String cover;

    private String title;

    private String topic;

    private Integer type;

    @JsonProperty("creator_nickname")
    private String creatorNickname;

    @JsonProperty("creator_avatar")
    private String creatorAvatar;

    @JsonProperty("create_time")
    private String createTime;

    @JsonProperty("update_time")
    private String updateTime;

    @JsonProperty("like_total")
    private Long likeTotal;

    @JsonProperty("collect_total")
    private Long collectTotal;

    @JsonProperty("comment_total")
    private Long commentTotal;
}
