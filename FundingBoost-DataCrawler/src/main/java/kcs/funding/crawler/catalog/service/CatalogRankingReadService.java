package kcs.funding.crawler.catalog.service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogRankingReadService {

    private static final Pattern SCHEMA_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");

    private final JdbcTemplate jdbcTemplate;

    @Value("${catalog.ranking.enabled:true}")
    private boolean rankingEnabled;

    @Value("${catalog.ranking.source-schema-name:fundingboost}")
    private String sourceSchemaName;

    @Value("${catalog.ranking.item-schema-name:item}")
    private String itemSchemaName;

    public List<RankingScore> findRankingScores(String rankingType, String audience, String priceRange, int limit) {
        if (!rankingEnabled || limit <= 0) {
            return List.of();
        }

        String sourceSchema = validateSchemaName(sourceSchemaName);
        String itemSchema = validateSchemaName(itemSchemaName);
        String sql = buildSql(sourceSchema, itemSchema, rankingType, audience, priceRange);

        try {
            return jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> new RankingScore(
                            rs.getLong("item_id"),
                            rs.getLong("score")
                    ),
                    limit
            );
        } catch (Exception exception) {
            log.warn("ranking query failed; fallback to latest items, type={}, audience={}, priceRange={}, message={}",
                    rankingType, audience, priceRange, exception.getMessage());
            return List.of();
        }
    }

    private String buildSql(String sourceSchema, String itemSchema, String rankingType, String audience, String priceRange) {
        String genderFilter = buildGenderFilter(audience);
        String priceFilter = buildPriceFilter(priceRange);

        return switch (rankingType) {
            case "purchase" -> """
                    SELECT oi.item_id AS item_id, COALESCE(SUM(oi.quantity), 0) AS score
                    FROM `%s`.`order_item` oi
                    JOIN `%s`.`orders` o ON oi.order_id = o.order_id
                    JOIN `%s`.`member` m ON o.member_id = m.member_id
                    JOIN `%s`.`item` i ON oi.item_id = i.item_id
                    WHERE 1 = 1
                    %s
                    %s
                    GROUP BY oi.item_id
                    ORDER BY score DESC, oi.item_id DESC
                    LIMIT ?
                    """.formatted(sourceSchema, sourceSchema, sourceSchema, itemSchema, genderFilter, priceFilter);
            case "wish" -> """
                    SELECT b.item_id AS item_id, COUNT(*) AS score
                    FROM `%s`.`bookmark` b
                    JOIN `%s`.`member` m ON b.member_id = m.member_id
                    JOIN `%s`.`item` i ON b.item_id = i.item_id
                    WHERE 1 = 1
                    %s
                    %s
                    GROUP BY b.item_id
                    ORDER BY score DESC, b.item_id DESC
                    LIMIT ?
                    """.formatted(sourceSchema, sourceSchema, itemSchema, genderFilter, priceFilter);
            default -> """
                    SELECT fi.item_id AS item_id, COUNT(*) AS score
                    FROM `%s`.`funding_item` fi
                    JOIN `%s`.`funding` f ON fi.funding_id = f.funding_id
                    JOIN `%s`.`member` m ON f.member_id = m.member_id
                    JOIN `%s`.`item` i ON fi.item_id = i.item_id
                    WHERE 1 = 1
                    %s
                    %s
                    GROUP BY fi.item_id
                    ORDER BY score DESC, fi.item_id DESC
                    LIMIT ?
                    """.formatted(sourceSchema, sourceSchema, sourceSchema, itemSchema, genderFilter, priceFilter);
        };
    }

    private String buildGenderFilter(String audience) {
        return switch (normalize(audience)) {
            case "man", "male" -> " AND m.gender = 'MAN'";
            case "woman", "female" -> " AND m.gender = 'WOMAN'";
            default -> "";
        };
    }

    private String buildPriceFilter(String priceRange) {
        return switch (normalize(priceRange)) {
            case "under10k" -> " AND i.item_price < 10000";
            case "10kto30k" -> " AND i.item_price >= 10000 AND i.item_price < 30000";
            case "30kto50k" -> " AND i.item_price >= 30000 AND i.item_price < 50000";
            case "over50k" -> " AND i.item_price >= 50000";
            default -> "";
        };
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String validateSchemaName(String name) {
        if (name == null || !SCHEMA_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid schema name: " + name);
        }
        return name;
    }

    public record RankingScore(Long itemId, long score) {
    }
}
