package kcs.funding.fundingboost.elasticsearch.entity;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kcs.funding.fundingboost.domain.entity.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "search_keyword_log",
        indexes = {
                @Index(name = "idx_search_keyword_log_keyword_created", columnList = "normalized_keyword, created_date"),
                @Index(name = "idx_search_keyword_log_created", columnList = "created_date")
        }
)
public class SearchKeywordLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "search_keyword_log_id")
    private Long searchKeywordLogId;

    @Column(name = "raw_keyword", length = 200)
    private String rawKeyword;

    @Column(name = "normalized_keyword", length = 120, nullable = false)
    private String normalizedKeyword;

    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @Column(name = "top_category", length = 40)
    private String topCategory;

    public static SearchKeywordLog create(String rawKeyword, String normalizedKeyword, int resultCount, String topCategory) {
        SearchKeywordLog log = new SearchKeywordLog();
        log.rawKeyword = rawKeyword;
        log.normalizedKeyword = normalizedKeyword;
        log.resultCount = Math.max(resultCount, 0);
        log.topCategory = topCategory;
        return log;
    }
}
