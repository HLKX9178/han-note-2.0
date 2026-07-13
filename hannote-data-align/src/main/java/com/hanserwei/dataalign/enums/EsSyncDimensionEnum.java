package com.hanserwei.dataalign.enums;

/**
 * 计数对齐后需刷新的 ES 索引维度.
 *
 * <p>由各计数对齐处理器声明：对齐完某条计数后，是否/按何维度通知搜索服务重建文档，
 * 以刷新 ES 索引中冗余的计数字段。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
public enum EsSyncDimensionEnum {

    /** 笔记维度：重建 note 文档（如笔记被点赞数、被收藏数） */
    NOTE,

    /** 用户维度：重建 user 文档（如用户发布笔记数、粉丝数） */
    USER,

    /** 不涉及 ES 索引（如关注数、用户获赞/获藏数——均不在索引中） */
    NONE
}
