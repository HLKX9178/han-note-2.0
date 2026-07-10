package com.hanserwei.framework.common.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 分页响应参数.
 *
 * <p>继承 {@link Response}，在通用响应结构（success / message / errorCode / data）之上，
 * 追加分页元信息（当前页码、总数据量、每页数量、总页数）。列表类接口统一返回此类型，
 * 前端据此渲染分页组件。
 *
 * <p>默认每页 10 条。越界页允许 {@code data == null}（分页元信息仍正确）。
 *
 * @param <T> 分页数据元素类型
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageResponse<T> extends Response<List<T>> {

    /** 默认每页数据量 */
    private static final long DEFAULT_PAGE_SIZE = 10L;

    /** 当前页码 */
    private long pageNo;

    /** 总数据量 */
    private long totalCount;

    /** 每页展示的数据量 */
    private long pageSize;

    /** 总页数 */
    private long totalPage;

    /**
     * 构建分页成功响应（每页默认 10 条）.
     *
     * @param data       当前页数据（允许为 {@code null}，表示无数据或越界页）
     * @param pageNo     当前页码
     * @param totalCount 总数据量
     * @param <T>        元素类型
     * @return 分页响应
     */
    public static <T> PageResponse<T> success(List<T> data, long pageNo, long totalCount) {
        return success(data, pageNo, totalCount, DEFAULT_PAGE_SIZE);
    }

    /**
     * 构建分页成功响应（指定每页数量）.
     *
     * @param data       当前页数据（允许为 {@code null}）
     * @param pageNo     当前页码
     * @param totalCount 总数据量
     * @param pageSize   每页数据量
     * @param <T>        元素类型
     * @return 分页响应
     */
    public static <T> PageResponse<T> success(List<T> data, long pageNo, long totalCount, long pageSize) {
        PageResponse<T> pageResponse = new PageResponse<>();
        pageResponse.setSuccess(true);
        pageResponse.setData(data);
        pageResponse.setPageNo(pageNo);
        pageResponse.setTotalCount(totalCount);
        pageResponse.setPageSize(pageSize);
        pageResponse.setTotalPage(getTotalPage(totalCount, pageSize));
        return pageResponse;
    }

    /**
     * 计算总页数（向上取整）.
     *
     * @param totalCount 总数据量
     * @param pageSize   每页数据量
     * @return 总页数；{@code pageSize == 0} 时返回 0
     */
    public static long getTotalPage(long totalCount, long pageSize) {
        return pageSize == 0 ? 0 : (totalCount + pageSize - 1) / pageSize;
    }

    /**
     * 计算分页查询的偏移量（offset）.
     *
     * @param pageNo   当前页码（小于 1 时归一到第一页）
     * @param pageSize 每页数据量
     * @return 偏移量
     */
    public static long getOffset(long pageNo, long pageSize) {
        if (pageNo < 1) {
            pageNo = 1;
        }
        return (pageNo - 1) * pageSize;
    }
}
