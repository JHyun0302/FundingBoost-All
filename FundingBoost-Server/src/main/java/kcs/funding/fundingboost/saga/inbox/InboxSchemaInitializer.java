package kcs.funding.fundingboost.saga.inbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.inbox", name = "schema-bootstrap-enabled", havingValue = "true", matchIfMissing = true)
public class InboxSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @jakarta.annotation.PostConstruct
    public void initialize() {
        String schema = resolveCurrentSchema();

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `%s`.`inbox_event` (
                    `inbox_event_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Inbox 이벤트 PK',
                    `created_date` DATETIME(6) NULL COMMENT '생성 일시',
                    `modified_date` DATETIME(6) NULL COMMENT '수정 일시',
                    `consumer_name` VARCHAR(80) NOT NULL COMMENT '이 이벤트를 소비하는 논리 consumer 이름',
                    `topic` VARCHAR(120) NOT NULL COMMENT 'Kafka topic 이름',
                    `event_key` VARCHAR(160) NOT NULL COMMENT 'Kafka event key',
                    `event_type` VARCHAR(120) NOT NULL COMMENT '도메인 이벤트 타입',
                    `status` VARCHAR(20) NOT NULL COMMENT 'Inbox 처리 상태 (RECEIVED, COMPLETED, FAILED)',
                    `payload` LONGTEXT NOT NULL COMMENT '원본 이벤트 payload',
                    `last_error` VARCHAR(500) NULL COMMENT '마지막 처리 실패 메시지',
                    `processed_at` DATETIME(6) NULL COMMENT '마지막 처리 완료/실패 시각',
                    PRIMARY KEY (`inbox_event_id`),
                    UNIQUE KEY `uk_inbox_event_consumer_topic_key` (`consumer_name`, `topic`, `event_key`),
                    KEY `idx_inbox_event_status` (`status`),
                    KEY `idx_inbox_event_processed_at` (`processed_at`)
                ) ENGINE=InnoDB COMMENT='도메인 이벤트 inbox 테이블 (idempotent consumer용)'
                """.formatted(schema));

        log.info("inbox schema bootstrap completed: schema={}", schema);
    }

    private String resolveCurrentSchema() {
        try {
            String schema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            if (schema == null || schema.isBlank()) {
                return "fundingboost";
            }
            return schema;
        } catch (Exception exception) {
            log.warn("failed to resolve current schema for inbox bootstrap; fallback=fundingboost", exception);
            return "fundingboost";
        }
    }
}
