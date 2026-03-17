package kcs.funding.fundingboost.init;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.schema-comment", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchemaCommentInitializer {

    private static final String CURRENT_SCHEMA_ALIAS = "__current_schema__";
    private static final String DEFAULT_APP_SCHEMA = "fundingboost";
    private static final String ORDERS_PAYMENT_INTENT_UNIQUE_INDEX = "uk_orders_payment_intent_key";
    private static final String ORDERS_PAYMENT_INTENT_COLUMN = "payment_intent_key";
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final Pattern CURRENT_TIMESTAMP_PATTERN =
            Pattern.compile("(?i)^CURRENT_TIMESTAMP(?:\\([0-9]+\\))?$");
    private static final Pattern BIT_LITERAL_PATTERN = Pattern.compile("(?i)^b'[01]+'$");

    private final JdbcTemplate jdbcTemplate;

    @jakarta.annotation.PostConstruct
    public void applySchemaComments() {
        String currentSchema = resolveCurrentSchema();
        COMMENT_SPECS.forEach((tableRef, spec) -> {
            TableRef resolvedTableRef = tableRef.resolve(currentSchema);
            if (!tableExists(resolvedTableRef)) {
                log.warn("skip schema comment bootstrap: missing table {}.{}", resolvedTableRef.schema(),
                        resolvedTableRef.table());
                return;
            }
            applyTableComment(resolvedTableRef, spec.tableComment());
            applyColumnComments(resolvedTableRef, spec.columnComments());
        });
        ensureUniqueIndexes(currentSchema);
        log.info("schema comment bootstrap completed: {} table(s)", COMMENT_SPECS.size());
    }

    private String resolveCurrentSchema() {
        try {
            String schema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            if (schema == null || schema.isBlank()) {
                return DEFAULT_APP_SCHEMA;
            }
            return validateIdentifier(schema);
        } catch (Exception e) {
            log.warn("failed to resolve current schema; fallback={}", DEFAULT_APP_SCHEMA, e);
            return DEFAULT_APP_SCHEMA;
        }
    }

    private boolean tableExists(TableRef tableRef) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.TABLES
                        WHERE TABLE_SCHEMA = ?
                          AND TABLE_NAME = ?
                        """,
                Integer.class,
                tableRef.schema(),
                tableRef.table()
        );
        return count != null && count > 0;
    }

    private void applyTableComment(TableRef tableRef, String tableComment) {
        String sql = """
                ALTER TABLE `%s`.`%s`
                COMMENT = %s
                """.formatted(tableRef.schema(), tableRef.table(), quote(tableComment));
        jdbcTemplate.execute(sql);
    }

    private void applyColumnComments(TableRef tableRef, Map<String, String> columnComments) {
        columnComments.forEach((columnName, comment) -> {
            ColumnMeta columnMeta = loadColumnMeta(tableRef, columnName);
            if (columnMeta == null) {
                log.warn("skip column comment bootstrap: missing column {}.{}.{}", tableRef.schema(), tableRef.table(),
                        columnName);
                return;
            }

            String sql = """
                    ALTER TABLE `%s`.`%s`
                    MODIFY COLUMN `%s` %s COMMENT %s
                    """.formatted(
                    tableRef.schema(),
                    tableRef.table(),
                    columnName,
                    buildColumnDefinition(columnMeta),
                    quote(comment)
            );
            jdbcTemplate.execute(sql);
        });
    }

    private void ensureUniqueIndexes(String currentSchema) {
        TableRef orders = new TableRef(currentSchema, "orders");
        if (!tableExists(orders)) {
            return;
        }
        ensureUniqueIndex(
                orders,
                ORDERS_PAYMENT_INTENT_UNIQUE_INDEX,
                ORDERS_PAYMENT_INTENT_COLUMN
        );
    }

    private void ensureUniqueIndex(TableRef tableRef, String indexName, String columnName) {
        if (uniqueIndexOnColumnExists(tableRef, columnName)) {
            return;
        }

        String sql = """
                ALTER TABLE `%s`.`%s`
                ADD UNIQUE INDEX `%s` (`%s`)
                """.formatted(
                tableRef.schema(),
                tableRef.table(),
                validateIdentifier(indexName),
                validateIdentifier(columnName)
        );
        jdbcTemplate.execute(sql);
        log.info("added unique index {} on {}.{}({})",
                indexName, tableRef.schema(), tableRef.table(), columnName);
    }

    private boolean uniqueIndexOnColumnExists(TableRef tableRef, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.STATISTICS
                        WHERE TABLE_SCHEMA = ?
                          AND TABLE_NAME = ?
                          AND COLUMN_NAME = ?
                          AND NON_UNIQUE = 0
                          AND INDEX_NAME <> 'PRIMARY'
                        """,
                Integer.class,
                tableRef.schema(),
                tableRef.table(),
                validateIdentifier(columnName)
        );
        return count != null && count > 0;
    }

    private ColumnMeta loadColumnMeta(TableRef tableRef, String columnName) {
        List<ColumnMeta> metas = jdbcTemplate.query(
                """
                        SELECT COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = ?
                          AND TABLE_NAME = ?
                          AND COLUMN_NAME = ?
                        """,
                (resultSet, rowNum) -> new ColumnMeta(
                        resultSet.getString("COLUMN_TYPE"),
                        "YES".equalsIgnoreCase(resultSet.getString("IS_NULLABLE")),
                        resultSet.getString("COLUMN_DEFAULT"),
                        resultSet.getString("EXTRA")
                ),
                tableRef.schema(),
                tableRef.table(),
                columnName
        );
        return metas.isEmpty() ? null : metas.get(0);
    }

    private String buildColumnDefinition(ColumnMeta columnMeta) {
        StringBuilder definition = new StringBuilder();
        definition.append(columnMeta.columnType());
        definition.append(columnMeta.nullable() ? " NULL" : " NOT NULL");

        if (columnMeta.columnDefault() != null) {
            definition.append(" DEFAULT ");
            if (isSqlExpression(columnMeta.columnDefault())) {
                definition.append(columnMeta.columnDefault());
            } else {
                definition.append(quote(columnMeta.columnDefault()));
            }
        }

        if (columnMeta.extra() != null && !columnMeta.extra().isBlank()) {
            definition.append(" ").append(columnMeta.extra());
        }
        return definition.toString();
    }

    private boolean isSqlExpression(String defaultValue) {
        String trimmed = defaultValue.trim();
        return CURRENT_TIMESTAMP_PATTERN.matcher(trimmed).matches()
                || BIT_LITERAL_PATTERN.matcher(trimmed).matches();
    }

    private static String quote(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'";
    }

    private static Map<TableRef, TableCommentSpec> buildCommentSpecs() {
        Map<TableRef, TableCommentSpec> specs = new LinkedHashMap<>();

        specs.put(
                new TableRef(CURRENT_SCHEMA_ALIAS, "bookmark"),
                new TableCommentSpec(
                        "회원 위시리스트(북마크) 테이블",
                        comments(
                                "favorite_id", "북마크 PK",
                                "created_date", "생성 일시",
                                "modified_date", "수정 일시",
                                "item_id", "북마크한 상품 ID",
                                "member_id", "북마크한 회원 ID"
                        )
                )
        );

        specs.put(
                new TableRef(CURRENT_SCHEMA_ALIAS, "contributor"),
                new TableCommentSpec(
                        "펀딩 참여(기여) 이력 테이블",
                        comments(
                                "contributor_id", "펀딩 참여 PK",
                                "created_date", "생성 일시",
                                "modified_date", "수정 일시",
                                "contributor_price", "참여 금액",
                                "funding_id", "대상 펀딩 ID",
                                "member_id", "참여 회원 ID"
                        )
                )
        );

        specs.put(
                new TableRef(CURRENT_SCHEMA_ALIAS, "delivery"),
                new TableCommentSpec(
                        "회원 배송지 정보 테이블",
                        comments(
                                "delivery_id", "배송지 PK",
                                "created_date", "생성 일시",
                                "modified_date", "수정 일시",
                                "address", "배송지 주소",
                                "customer_name", "수령인 이름",
                                "phone_number", "수령인 연락처",
                                "member_id", "배송지 소유 회원 ID",
                                "delivery_memo", "배송 메모",
                                "postal_code", "우편번호"
                        )
                )
        );

        specs.put(
                new TableRef(CURRENT_SCHEMA_ALIAS, "friend_pay_barcode_token"),
                new TableCommentSpec(
                        "친구 결제 바코드 토큰 테이블",
                        comments(
                                "friend_pay_barcode_token_id", "친구 결제 바코드 토큰 PK",
                                "created_date", "생성 일시",
                                "modified_date", "수정 일시",
                                "barcode_token", "발급된 바코드 토큰 문자열",
                                "expires_at", "토큰 만료 시각",
                                "funding_price", "요청된 펀딩 결제 금액",
                                "token_status", "토큰 상태(PENDING/USED/EXPIRED)",
                                "used_at", "토큰 사용 시각",
                                "using_point", "결제 시 사용 포인트 금액",
                                "funding_id", "대상 펀딩 ID",
                                "member_id", "토큰 발급 회원 ID"
                        )
                )
        );

        specs.put(
                new TableRef(CURRENT_SCHEMA_ALIAS, "funding"),
                new TableCommentSpec(
                        "펀딩 본문 테이블",
                        comments(
                                "funding_id", "펀딩 PK",
                                "created_date", "생성 일시",
                                "modified_date", "수정 일시",
                                "collect_price", "현재 모금 금액",
                                "deadline", "펀딩 마감 일시",
                                "funding_status", "펀딩 진행 상태(1:진행, 0:종료)",
                                "message", "펀딩 메시지",
                                "tag", "펀딩 태그(BIRTHDAY/GRADUATE/ETC)",
                                "total_price", "펀딩 목표 금액",
                                "member_id", "펀딩 생성 회원 ID",
                                "custom_tag", "사용자 정의 태그"
                        )
                )
        );

        specs.put(
                new TableRef(CURRENT_SCHEMA_ALIAS, "funding_item"),
                new TableCommentSpec(
                        "펀딩 대상 상품 테이블",
                        comments(
                                "funding_item_id", "펀딩 상품 PK",
                                "created_date", "생성 일시",
                                "modified_date", "수정 일시",
                                "finished_status", "해당 상품 펀딩 완료 여부",
                                "item_sequence", "펀딩 내 상품 순서",
                                "item_status", "펀딩 내 상품 활성 여부",
                                "funding_id", "소속 펀딩 ID",
                                "item_id", "상품 ID"
                        )
                )
        );

        specs.put(
                new TableRef(CURRENT_SCHEMA_ALIAS, "gift_hub_item"),
                new TableCommentSpec(
                        "장바구니(기프트허브) 상품 테이블",
                        comments(
                                "gift_hub_item_id", "장바구니 상품 PK",
                                "created_date", "생성 일시",
                                "modified_date", "수정 일시",
                                "quantity", "장바구니 담은 수량",
                                "option_name", "사용자가 선택한 옵션명",
                                "item_id", "상품 ID",
                                "member_id", "회원 ID"
                        )
                )
        );

        specs.put(
                new TableRef(CURRENT_SCHEMA_ALIAS, "member"),
                new TableCommentSpec(
                        "회원 정보 테이블",
                        comments(
                                "member_id", "회원 PK",
                                "created_date", "생성 일시",
                                "modified_date", "수정 일시",
                                "email", "로그인 이메일",
                                "gender", "성별(MAN/WOMAN/UNKNOWN)",
                                "kakao_id", "카카오 연동 식별자",
                                "member_role", "회원 권한(ROLE_USER/ROLE_ADMIN)",
                                "nick_name", "닉네임",
                                "password", "비밀번호 해시",
                                "point", "보유 포인트",
                                "profile_img_url", "프로필 이미지 URL"
                        )
                )
        );

        specs.put(
                new TableRef(CURRENT_SCHEMA_ALIAS, "notice"),
                new TableCommentSpec(
                        "공지사항 테이블",
                        comments(
                                "notice_id", "공지사항 PK",
                                "created_date", "생성 일시",
                                "modified_date", "수정 일시",
                                "body", "공지 본문",
                                "category", "공지 카테고리",
                                "title", "공지 제목"
                        )
                )
        );

        specs.put(
                new TableRef(CURRENT_SCHEMA_ALIAS, "order_item"),
                new TableCommentSpec(
                        "주문 상품 매핑 테이블",
                        comments(
                                "order_item_id", "주문 상품 PK",
                                "quantity", "주문 수량",
                                "option_name", "주문 당시 선택 옵션명",
                                "item_id", "주문한 상품 ID",
                                "order_id", "소속 주문 ID"
                        )
                )
        );

        specs.put(
                new TableRef(CURRENT_SCHEMA_ALIAS, "orders"),
                new TableCommentSpec(
                        "주문 테이블",
                        comments(
                                "order_id", "주문 PK",
                                "created_date", "생성 일시",
                                "modified_date", "수정 일시",
                                "total_price", "총 주문 금액",
                                "delivery_id", "배송지 ID",
                                "member_id", "주문 회원 ID",
                                "direct_paid_amount", "일반결제로 결제된 금액",
                                "funding_supported_amount", "펀딩 지원금으로 결제된 금액",
                                "point_used_amount", "포인트 사용 금액",
                                "source_funding_id", "주문 생성의 원천 펀딩 ID",
                                "payment_intent_key", "결제 의도 키(payment.payment_intent.intent_key)"
                        )
                )
        );

        specs.put(
                new TableRef(CURRENT_SCHEMA_ALIAS, "relationship"),
                new TableCommentSpec(
                        "회원 친구 관계 테이블",
                        comments(
                                "relation_id", "친구 관계 PK",
                                "created_date", "생성 일시",
                                "modified_date", "수정 일시",
                                "friend_id", "친구 회원 ID",
                                "member_id", "기준 회원 ID"
                        )
                )
        );

        specs.put(
                new TableRef(CURRENT_SCHEMA_ALIAS, "review"),
                new TableCommentSpec(
                        "상품 리뷰 테이블",
                        comments(
                                "review_id", "리뷰 PK",
                                "created_date", "생성 일시",
                                "modified_date", "수정 일시",
                                "content", "리뷰 내용",
                                "rating", "평점",
                                "item_id", "대상 상품 ID",
                                "member_id", "작성 회원 ID"
                        )
                )
        );

        specs.put(
                new TableRef(CURRENT_SCHEMA_ALIAS, "search_keyword_log"),
                new TableCommentSpec(
                        "검색어 로그 테이블",
                        comments(
                                "search_keyword_log_id", "검색 로그 PK",
                                "created_date", "생성 일시",
                                "modified_date", "수정 일시",
                                "raw_keyword", "사용자가 입력한 원문 검색어",
                                "normalized_keyword", "정규화된 검색어",
                                "result_count", "검색 결과 개수",
                                "top_category", "검색 결과 최다 카테고리"
                        )
                )
        );

        specs.put(
                new TableRef(CURRENT_SCHEMA_ALIAS, "support_faq"),
                new TableCommentSpec(
                        "고객센터 FAQ 테이블",
                        comments(
                                "faq_id", "FAQ PK",
                                "created_date", "생성 일시",
                                "modified_date", "수정 일시",
                                "answer", "답변",
                                "question", "질문",
                                "sort_order", "노출 순서"
                        )
                )
        );

        specs.put(
                new TableRef("item", "brand_crawl_target"),
                new TableCommentSpec(
                        "크롤링 대상 브랜드 설정 테이블",
                        comments(
                                "brand_target_id", "브랜드 타깃 PK",
                                "created_date", "생성 일시",
                                "modified_date", "수정 일시",
                                "brand_name", "브랜드 이름",
                                "brand_url", "브랜드 상품 목록 URL",
                                "category_name", "크롤링 카테고리 이름"
                        )
                )
        );

        specs.put(
                new TableRef("item", "item"),
                new TableCommentSpec(
                        "상품 마스터 테이블",
                        comments(
                                "item_id", "상품 PK",
                                "created_date", "생성 일시",
                                "modified_date", "수정 일시",
                                "brand_name", "브랜드 이름",
                                "category", "상품 카테고리",
                                "item_image_url", "상품 대표 이미지 URL",
                                "item_name", "상품명",
                                "item_price", "상품 가격",
                                "option_name", "옵션 원문 문자열",
                                "product_id", "외부 쇼핑몰 상품 식별자"
                        )
                )
        );

        return specs;
    }

    private static Map<String, String> comments(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("comments() arguments must be key/value pairs");
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    private static String validateIdentifier(String identifier) {
        if (!IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid identifier: " + identifier);
        }
        return identifier;
    }

    private record TableRef(String schema, String table) {
        private TableRef {
            schema = validateIdentifier(schema);
            table = validateIdentifier(table);
        }

        private TableRef resolve(String currentSchema) {
            if (CURRENT_SCHEMA_ALIAS.equals(schema)) {
                return new TableRef(currentSchema, table);
            }
            return this;
        }
    }

    private record TableCommentSpec(String tableComment, Map<String, String> columnComments) {
    }

    private record ColumnMeta(String columnType, boolean nullable, String columnDefault, String extra) {
    }

    private static final Map<TableRef, TableCommentSpec> COMMENT_SPECS = buildCommentSpecs();
}
