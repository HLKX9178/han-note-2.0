package com.hanserwei.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.util.NamedValue;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.framework.common.util.NumberUtils;
import com.hanserwei.search.enums.ResponseCodeEnum;
import com.hanserwei.search.index.UserIndex;
import com.hanserwei.search.model.document.UserDocument;
import com.hanserwei.search.model.vo.SearchUserReqVO;
import com.hanserwei.search.model.vo.SearchUserRspVO;
import com.hanserwei.search.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 用户搜索业务实现.
 *
 * <p>基于 ES {@code user} 索引，对昵称、小憨书号做多字段匹配，按粉丝数降序分页返回，
 * 并对昵称关键词高亮、粉丝数做友好格式化。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final ElasticsearchClient elasticsearchClient;

    /** 每页展示数量 */
    private static final int PAGE_SIZE = 10;

    @Override
    public PageResponse<SearchUserRspVO> searchUser(SearchUserReqVO searchUserReqVO) {
        String keyword = searchUserReqVO.getKeyword();
        Integer pageNo = searchUserReqVO.getPageNo();
        int from = (pageNo - 1) * PAGE_SIZE;

        List<SearchUserRspVO> rspVOS = new ArrayList<>();
        long total = 0;

        try {
            // 构建并执行搜索：multi_match(nickname, hannote_id) + 粉丝数降序 + 昵称高亮 + 分页
            SearchResponse<UserDocument> response = elasticsearchClient.search(s -> s
                            .index(UserIndex.NAME)
                            .query(q -> q.multiMatch(m -> m.query(keyword)
                                    .fields(UserIndex.FIELD_USER_NICKNAME, UserIndex.FIELD_USER_HANNOTE_ID)))
                            .sort(so -> so.field(f -> f.field(UserIndex.FIELD_USER_FANS_TOTAL).order(SortOrder.Desc)))
                            .from(from)
                            .size(PAGE_SIZE)
                            .highlight(h -> h.fields(NamedValue.of(UserIndex.FIELD_USER_NICKNAME,
                                    HighlightField.of(hf -> hf.preTags("<em>").postTags("</em>"))))),
                    UserDocument.class);

            if (Objects.nonNull(response.hits().total())) {
                total = response.hits().total().value();
            }
            log.info("==> 搜索用户命中文档总数: {}", total);

            for (Hit<UserDocument> hit : response.hits().hits()) {
                UserDocument doc = hit.source();
                if (Objects.isNull(doc)) {
                    continue;
                }

                long fansTotal = Objects.isNull(doc.getFansTotal()) ? 0L : doc.getFansTotal();

                rspVOS.add(SearchUserRspVO.builder()
                        .userId(doc.getId())
                        .nickname(doc.getNickname())
                        .highlightNickname(extractHighlight(hit, UserIndex.FIELD_USER_NICKNAME))
                        .avatar(doc.getAvatar())
                        .hannoteId(doc.getHannoteId())
                        .noteTotal(doc.getNoteTotal())
                        .fansTotal(NumberUtils.formatNumberString(fansTotal))
                        .build());
            }
        } catch (IOException e) {
            log.error("==> 搜索用户 Elasticsearch 异常: ", e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        return PageResponse.success(rspVOS, pageNo, total);
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
