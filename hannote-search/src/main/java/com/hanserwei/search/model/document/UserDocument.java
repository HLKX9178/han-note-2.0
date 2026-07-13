package com.hanserwei.search.model.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户索引文档.
 *
 * <p>ES {@code user} 索引命中文档的反序列化载体，由 {@code JacksonJsonpMapper}（Jackson 2）
 * 填充，字段通过 {@link JsonProperty} 映射索引的 snake_case 命名。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDocument {

    private Long id;

    private String nickname;

    private String avatar;

    @JsonProperty("hannote_id")
    private String hannoteId;

    @JsonProperty("note_total")
    private Integer noteTotal;

    @JsonProperty("fans_total")
    private Long fansTotal;
}
