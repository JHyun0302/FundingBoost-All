package kcs.funding.fundingboost.elasticsearch.repository;

import java.time.LocalDateTime;
import java.util.List;
import kcs.funding.fundingboost.elasticsearch.entity.SearchKeywordLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SearchKeywordLogRepository extends JpaRepository<SearchKeywordLog, Long> {

    @Query("select l.normalizedKeyword as keyword, count(l) as frequency "
            + "from SearchKeywordLog l "
            + "where l.createdDate >= :from and l.normalizedKeyword is not null and l.normalizedKeyword <> '' "
            + "group by l.normalizedKeyword "
            + "order by count(l) desc")
    List<KeywordFrequencyView> findTopKeywordsSince(@Param("from") LocalDateTime from, Pageable pageable);

    @Query("select l.topCategory as category, count(l) as frequency "
            + "from SearchKeywordLog l "
            + "where l.createdDate >= :from "
            + "and l.normalizedKeyword = :keyword "
            + "and l.topCategory is not null and l.topCategory <> '' "
            + "group by l.topCategory "
            + "order by count(l) desc")
    List<CategoryFrequencyView> findTopCategoriesByKeywordSince(
            @Param("keyword") String keyword,
            @Param("from") LocalDateTime from,
            Pageable pageable
    );

    long deleteByCreatedDateBefore(LocalDateTime cutoff);

    interface KeywordFrequencyView {
        String getKeyword();

        long getFrequency();
    }

    interface CategoryFrequencyView {
        String getCategory();

        long getFrequency();
    }
}
