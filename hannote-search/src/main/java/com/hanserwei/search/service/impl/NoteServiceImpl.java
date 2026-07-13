package com.hanserwei.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.util.NamedValue;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.framework.common.util.DateUtils;
import com.hanserwei.framework.common.util.NumberUtils;
import com.hanserwei.search.enums.NotePublishTimeRangeEnum;
import com.hanserwei.search.enums.NoteSortTypeEnum;
import com.hanserwei.search.enums.ResponseCodeEnum;
import com.hanserwei.search.index.NoteIndex;
import com.hanserwei.search.model.document.NoteDocument;
import com.hanserwei.search.model.vo.SearchNoteReqVO;
import com.hanserwei.search.model.vo.SearchNoteRspVO;
import com.hanserwei.search.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 笔记搜索业务实现.
 *
 * <p>基于 ES {@code note} 索引，对标题（权重 2）、话题做多字段匹配，支持按笔记类型、发布时间
 * 范围过滤，以及最新/最多点赞/评论/收藏排序切换；未指定排序时走 {@code function_score}
 * 综合评分（点赞 0.5 + 收藏 0.3 + 评论 0.2，均 {@code sqrt} 平滑）。标题关键词高亮，
 * 计数与更新时间做友好格式化。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final ElasticsearchClient elasticsearchClient;

    /** 每页展示数量 */
    private static final int PAGE_SIZE = 10;

    @Override
    public PageResponse<SearchNoteRspVO> searchNote(SearchNoteReqVO searchNoteReqVO) {
        String keyword = searchNoteReqVO.getKeyword();
        Integer pageNo = searchNoteReqVO.getPageNo();
        Integer type = searchNoteReqVO.getType();
        Integer sort = searchNoteReqVO.getSort();
        Integer publishTimeRange = searchNoteReqVO.getPublishTimeRange();
        int from = (pageNo - 1) * PAGE_SIZE;

        // 构建 bool 查询（关键词匹配 + 可选过滤条件）
        BoolQuery boolQuery = buildBoolQuery(keyword, type, publishTimeRange);

        // 构建搜索请求：分页 + 标题高亮，排序视是否指定 sort 而定
        SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                .index(NoteIndex.NAME)
                .from(from)
                .size(PAGE_SIZE)
                .highlight(h -> h.fields(NamedValue.of(NoteIndex.FIELD_NOTE_TITLE,
                        HighlightField.of(hf -> hf.preTags("<em>").postTags("</em>")))));

        NoteSortTypeEnum sortEnum = NoteSortTypeEnum.valueOf(sort);
        if (Objects.nonNull(sortEnum)) {
            // 指定字段排序：直接用 bool 查询 + 目标字段降序
            String sortField = switch (sortEnum) {
                case LATEST -> NoteIndex.FIELD_NOTE_CREATE_TIME;
                case MOST_LIKE -> NoteIndex.FIELD_NOTE_LIKE_TOTAL;
                case MOST_COMMENT -> NoteIndex.FIELD_NOTE_COMMENT_TOTAL;
                case MOST_COLLECT -> NoteIndex.FIELD_NOTE_COLLECT_TOTAL;
            };
            requestBuilder.query(q -> q.bool(boolQuery))
                    .sort(so -> so.field(f -> f.field(sortField).order(SortOrder.Desc)));
        } else {
            // 综合排序：function_score 自定义评分 + 按 _score 降序
            requestBuilder.query(q -> q.functionScore(fs -> fs
                            .query(iq -> iq.bool(boolQuery))
                            .functions(buildScoreFunctions())
                            .scoreMode(FunctionScoreMode.Sum)
                            .boostMode(FunctionBoostMode.Sum)))
                    .sort(so -> so.score(sc -> sc.order(SortOrder.Desc)));
        }

        SearchRequest searchRequest = requestBuilder.build();

        List<SearchNoteRspVO> rspVOS = new ArrayList<>();
        long total = 0;
        try {
            log.info("==> 笔记搜索请求: {}", searchRequest.toString());
            SearchResponse<NoteDocument> response = elasticsearchClient.search(searchRequest, NoteDocument.class);

            if (Objects.nonNull(response.hits().total())) {
                total = response.hits().total().value();
            }
            log.info("==> 搜索笔记命中文档总数: {}", total);

            for (Hit<NoteDocument> hit : response.hits().hits()) {
                NoteDocument doc = hit.source();
                if (Objects.isNull(doc)) {
                    continue;
                }
                rspVOS.add(toRspVO(doc, extractHighlight(hit, NoteIndex.FIELD_NOTE_TITLE)));
            }
        } catch (IOException e) {
            log.error("==> 搜索笔记 Elasticsearch 异常: ", e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        return PageResponse.success(rspVOS, pageNo, total);
    }

    /**
     * 构建 bool 查询：{@code must} 多字段匹配，按需追加类型、发布时间范围过滤。
     */
    private BoolQuery buildBoolQuery(String keyword, Integer type, Integer publishTimeRange) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // must：multi_match(title^2, topic)
        boolBuilder.must(q -> q.multiMatch(m -> m.query(keyword)
                .fields(NoteIndex.FIELD_NOTE_TITLE + "^2", NoteIndex.FIELD_NOTE_TOPIC)));

        // 笔记类型过滤（不影响相关性评分）
        if (Objects.nonNull(type)) {
            long typeValue = type.longValue();
            boolBuilder.filter(q -> q.term(t -> t.field(NoteIndex.FIELD_NOTE_TYPE).value(typeValue)));
        }

        // 发布时间范围过滤
        NotePublishTimeRangeEnum rangeEnum = NotePublishTimeRangeEnum.valueOf(publishTimeRange);
        if (Objects.nonNull(rangeEnum)) {
            String endTime = DateUtils.localDateTime2String(LocalDateTime.now());
            String startTime = switch (rangeEnum) {
                case DAY -> DateUtils.localDateTime2String(LocalDateTime.now().minusDays(1));
                case WEEK -> DateUtils.localDateTime2String(LocalDateTime.now().minusWeeks(1));
                case HALF_YEAR -> DateUtils.localDateTime2String(LocalDateTime.now().minusMonths(6));
            };
            if (StringUtils.isNotBlank(startTime)) {
                boolBuilder.filter(q -> q.range(r -> r.date(d -> d
                        .field(NoteIndex.FIELD_NOTE_CREATE_TIME)
                        .gte(startTime)
                        .lte(endTime))));
            }
        }

        return boolBuilder.build();
    }

    /**
     * 综合排序的评分函数：点赞 0.5 / 收藏 0.3 / 评论 0.2，均以 {@code sqrt} 平滑、缺失记 0。
     */
    private List<FunctionScore> buildScoreFunctions() {
        return List.of(
                scoreFunction(NoteIndex.FIELD_NOTE_LIKE_TOTAL, 0.5),
                scoreFunction(NoteIndex.FIELD_NOTE_COLLECT_TOTAL, 0.3),
                scoreFunction(NoteIndex.FIELD_NOTE_COMMENT_TOTAL, 0.2));
    }

    private FunctionScore scoreFunction(String field, double factor) {
        return FunctionScore.of(fs -> fs.fieldValueFactor(fv -> fv
                .field(field)
                .factor(factor)
                .modifier(FieldValueFactorModifier.Sqrt)
                .missing(0.0)));
    }

    /**
     * 命中文档 + 高亮标题 → 返参 VO，计数与更新时间做友好格式化。
     */
    private SearchNoteRspVO toRspVO(NoteDocument doc, String highlightTitle) {
        String updateTime = null;
        if (StringUtils.isNotBlank(doc.getUpdateTime())) {
            updateTime = DateUtils.formatRelativeTime(DateUtils.parseFromEsDateTime(doc.getUpdateTime()));
        }

        return SearchNoteRspVO.builder()
                .noteId(doc.getId())
                .cover(doc.getCover())
                .title(doc.getTitle())
                .highlightTitle(highlightTitle)
                .avatar(doc.getCreatorAvatar())
                .nickname(doc.getCreatorNickname())
                .updateTime(updateTime)
                .likeTotal(NumberUtils.formatNumberString(nullToZero(doc.getLikeTotal())))
                .commentTotal(NumberUtils.formatNumberString(nullToZero(doc.getCommentTotal())))
                .collectTotal(NumberUtils.formatNumberString(nullToZero(doc.getCollectTotal())))
                .build();
    }

    private static long nullToZero(Long value) {
        return Objects.isNull(value) ? 0L : value;
    }

    /**
     * 从命中结果中提取指定字段的首个高亮片段，无则返回 {@code null}。
     */
    private static String extractHighlight(Hit<?> hit, String field) {
        Map<String, List<String>> highlight = hit.highlight();
        if (Objects.nonNull(highlight)) {
            List<String> fragments = highlight.get(field);
            if (Objects.nonNull(fragments) && !fragments.isEmpty()) {
                return fragments.getFirst();
            }
        }
        return null;
    }
}
