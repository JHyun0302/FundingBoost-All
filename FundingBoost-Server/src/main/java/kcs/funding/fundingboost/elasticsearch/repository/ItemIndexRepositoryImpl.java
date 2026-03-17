package kcs.funding.fundingboost.elasticsearch.repository;

import java.util.List;
import java.util.Objects;
import kcs.funding.fundingboost.elasticsearch.index.ItemIndex;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ItemIndexRepositoryImpl implements ItemIndexRepositoryCustom {
    private final ElasticsearchOperations elasticSearchOperations;

    @Override
    public Slice<ItemIndex> findByCategoryOrItemName(String keyword, Pageable pageable) {
        return findByKeywords(List.of(keyword), pageable);
    }

    @Override
    public Slice<ItemIndex> findByKeywords(List<String> keywords, Pageable pageable) {
        List<String> normalizedKeywords = keywords == null
                ? List.of()
                : keywords.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(keyword -> !keyword.isBlank())
                        .distinct()
                        .toList();

        if (normalizedKeywords.isEmpty()) {
            return new SliceImpl<>(List.of(), pageable, false);
        }

        Criteria criteria = null;
        for (String keyword : normalizedKeywords) {
            Criteria keywordCriteria = new Criteria("item_name").matches(keyword)
                    .or("brand_name").matches(keyword)
                    .or("option_name").matches(keyword)
                    .or("category").matches(keyword);
            criteria = criteria == null ? keywordCriteria : criteria.or(keywordCriteria);
        }

        CriteriaQuery query = new CriteriaQuery(criteria)
                .setPageable(overFetchPageable(pageable, relevanceSort()));

        return searchAsSlice(query, pageable);
    }

    @Override
    public Slice<ItemIndex> findByCategory(String keyword, Pageable pageable) {
        Criteria criteria = new Criteria("category").is(keyword);
        CriteriaQuery query = new CriteriaQuery(criteria)
                .setPageable(overFetchPageable(pageable, defaultSort()));
        return searchAsSlice(query, pageable);
    }

    private Slice<ItemIndex> searchAsSlice(CriteriaQuery query, Pageable pageable) {
        SearchHits<ItemIndex> searchHits = elasticSearchOperations.search(query, ItemIndex.class);
        List<ItemIndex> items = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        boolean hasNext = items.size() > pageable.getPageSize();
        if (hasNext) {
            items = items.subList(0, pageable.getPageSize());
        }

        return new SliceImpl<>(items, pageable, hasNext);
    }

    private Pageable overFetchPageable(Pageable pageable, Sort fallbackSort) {
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : (fallbackSort == null ? Sort.unsorted() : fallbackSort);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize() + 1, sort);
    }

    private Sort defaultSort() {
        return Sort.by(Sort.Order.desc("itemId"));
    }

    private Sort relevanceSort() {
        return Sort.by(Sort.Order.desc("_score"), Sort.Order.desc("itemId"));
    }
}
