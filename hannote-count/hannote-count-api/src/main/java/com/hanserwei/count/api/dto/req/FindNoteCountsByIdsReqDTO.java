package com.hanserwei.count.api.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量查询笔记维度计数请求.
 *
 * @author hanserwei
 * @date 2026/07/18
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindNoteCountsByIdsReqDTO {

    /** 笔记 ID 集合 */
    @NotNull(message = "笔记 ID 集合不能为空")
    @Size(min = 1, max = 20, message = "笔记 ID 集合大小必须在 1~20 之间")
    private List<Long> noteIds;
}
