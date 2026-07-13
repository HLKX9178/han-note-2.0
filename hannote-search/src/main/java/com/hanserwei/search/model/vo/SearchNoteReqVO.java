package com.hanserwei.search.model.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索笔记入参.
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchNoteReqVO {

    /** 搜索关键词 */
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;

    /** 页码，默认第一页 */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNo = 1;

    /** 笔记类型：null 综合 / 0 图文 / 1 视频 */
    private Integer type;

    /** 排序：null 综合 / 0 最新 / 1 最多点赞 / 2 最多评论 / 3 最多收藏 */
    private Integer sort;

    /** 发布时间范围：null 不限 / 0 一天内 / 1 一周内 / 2 半年内 */
    private Integer publishTimeRange;
}
