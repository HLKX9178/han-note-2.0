package com.hanserwei.search.model.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索用户入参.
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchUserReqVO {

    /** 搜索关键词 */
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;

    /** 页码，默认第一页 */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNo = 1;
}
