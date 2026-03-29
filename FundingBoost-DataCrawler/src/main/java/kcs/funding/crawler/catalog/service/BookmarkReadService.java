package kcs.funding.crawler.catalog.service;

import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookmarkReadService {

    private static final Pattern SCHEMA_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");

    private final JdbcTemplate jdbcTemplate;

    @Value("${catalog.bookmark.enabled:true}")
    private boolean bookmarkEnabled;

    @Value("${catalog.bookmark.schema-name:fundingboost}")
    private String bookmarkSchemaName;

    public boolean isBookmarked(Long memberId, Long itemId) {
        if (!bookmarkEnabled || memberId == null || itemId == null) {
            return false;
        }

        String safeSchemaName = validateSchemaName(bookmarkSchemaName);
        String sql = """
                SELECT EXISTS(
                    SELECT 1
                    FROM `%s`.`bookmark`
                    WHERE member_id = ? AND item_id = ?
                    LIMIT 1
                )
                """.formatted(safeSchemaName);

        try {
            Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, memberId, itemId);
            return Boolean.TRUE.equals(exists);
        } catch (Exception exception) {
            log.warn("bookmark lookup failed; fallback=false, message={}", exception.getMessage());
            return false;
        }
    }

    private static String validateSchemaName(String name) {
        if (name == null || !SCHEMA_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid bookmark schema name: " + name);
        }
        return name;
    }
}
