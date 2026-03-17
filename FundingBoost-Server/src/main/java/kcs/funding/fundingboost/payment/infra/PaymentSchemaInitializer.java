package kcs.funding.fundingboost.payment.infra;

import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.payment", name = "schema-bootstrap-enabled", havingValue = "true")
public class PaymentSchemaInitializer {

    private static final Pattern SCHEMA_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.payment.schema-name:payment}")
    private String schemaName;

    @jakarta.annotation.PostConstruct
    public void initializeSchema() {
        String safeSchemaName = validateSchemaName(schemaName);

        jdbcTemplate.execute("""
                CREATE DATABASE IF NOT EXISTS `%s`
                CHARACTER SET utf8mb4
                COLLATE utf8mb4_unicode_ci
                """.formatted(safeSchemaName));

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `%s`.`payment_intent` (
                    `payment_intent_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '결제 의도 PK',
                    `intent_key` VARCHAR(80) NOT NULL COMMENT '외부/내부 추적용 결제 의도 키 (pi_...)',
                    `member_id` BIGINT NOT NULL COMMENT '결제 요청 사용자 ID',
                    `intent_type` VARCHAR(40) NOT NULL COMMENT '결제 유형 (ORDER_CART, ORDER_NOW, FUNDING_REMAIN)',
                    `reference_id` BIGINT NULL COMMENT '원본 도메인 참조 ID (예: itemId, fundingItemId)',
                    `order_id` BIGINT NULL COMMENT '주문 확정 후 연결되는 orders.order_id',
                    `currency` VARCHAR(10) NOT NULL COMMENT '통화 코드 (기본 KRW)',
                    `total_amount` INT NOT NULL COMMENT '총 결제 금액 (포인트+일반결제+펀딩지원금)',
                    `point_amount` INT NOT NULL COMMENT '포인트 결제 금액',
                    `pg_amount` INT NOT NULL COMMENT '일반결제(Mock PG) 금액',
                    `funding_supported_amount` INT NOT NULL COMMENT '펀딩 지원금으로 상계된 금액',
                    `status` VARCHAR(40) NOT NULL COMMENT '결제 상태머신 현재 상태',
                    `pg_provider` VARCHAR(40) NULL COMMENT 'PG 제공자 이름 (예: MOCK-PG)',
                    `pg_transaction_id` VARCHAR(120) NULL COMMENT 'PG 승인/거래 식별자',
                    `failure_code` VARCHAR(50) NULL COMMENT '실패 코드',
                    `failure_message` VARCHAR(255) NULL COMMENT '실패 상세 메시지',
                    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '레코드 생성 시각',
                    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '레코드 수정 시각',
                    PRIMARY KEY (`payment_intent_id`),
                    UNIQUE KEY `uk_payment_intent_intent_key` (`intent_key`),
                    KEY `idx_payment_intent_member_id` (`member_id`),
                    KEY `idx_payment_intent_status` (`status`),
                    KEY `idx_payment_intent_created_at` (`created_at`)
                ) ENGINE=InnoDB COMMENT='결제 의도 본문 테이블 (1건 결제의 현재 상태/금액 스냅샷)'
                """.formatted(safeSchemaName));

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `%s`.`payment_attempt` (
                    `payment_attempt_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '결제 시도 이력 PK',
                    `payment_intent_id` BIGINT NOT NULL COMMENT 'payment_intent.payment_intent_id FK 성격',
                    `attempt_no` INT NOT NULL COMMENT '동일 intent 내 시도 순번 (1부터 증가)',
                    `stage` VARCHAR(40) NOT NULL COMMENT '상태머신 단계명 (INTENT_CREATED, PG_REQUESTED ...)',
                    `stage_status` VARCHAR(20) NOT NULL COMMENT '단계 처리 결과 (SUCCESS/FAILED)',
                    `provider` VARCHAR(40) NULL COMMENT '처리한 제공자 (예: MOCK-PG)',
                    `provider_transaction_id` VARCHAR(120) NULL COMMENT '제공자 거래 ID',
                    `error_code` VARCHAR(50) NULL COMMENT '실패 코드',
                    `error_message` VARCHAR(255) NULL COMMENT '실패 메시지',
                    `requested_amount` INT NOT NULL COMMENT '해당 단계에서 처리하려는 금액',
                    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '시도 기록 생성 시각',
                    PRIMARY KEY (`payment_attempt_id`),
                    KEY `idx_payment_attempt_payment_intent_id` (`payment_intent_id`),
                    KEY `idx_payment_attempt_created_at` (`created_at`)
                ) ENGINE=InnoDB COMMENT='결제 단계별 시도 이력 테이블 (append-only)'
                """.formatted(safeSchemaName));

        applyTableComments(safeSchemaName);

        log.info("payment schema bootstrap completed: schema={}", safeSchemaName);
    }

    private void applyTableComments(String safeSchemaName) {
        String paymentIntentTable = "`" + safeSchemaName + "`.`payment_intent`";
        String paymentAttemptTable = "`" + safeSchemaName + "`.`payment_attempt`";

        jdbcTemplate.execute("""
                ALTER TABLE %s
                COMMENT = '결제 의도 본문 테이블 (1건 결제의 현재 상태/금액 스냅샷)'
                """.formatted(paymentIntentTable));
        jdbcTemplate.execute("""
                ALTER TABLE %s
                    MODIFY COLUMN `payment_intent_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '결제 의도 PK',
                    MODIFY COLUMN `intent_key` VARCHAR(80) NOT NULL COMMENT '외부/내부 추적용 결제 의도 키 (pi_...)',
                    MODIFY COLUMN `member_id` BIGINT NOT NULL COMMENT '결제 요청 사용자 ID',
                    MODIFY COLUMN `intent_type` VARCHAR(40) NOT NULL COMMENT '결제 유형 (ORDER_CART, ORDER_NOW, FUNDING_REMAIN)',
                    MODIFY COLUMN `reference_id` BIGINT NULL COMMENT '원본 도메인 참조 ID (예: itemId, fundingItemId)',
                    MODIFY COLUMN `order_id` BIGINT NULL COMMENT '주문 확정 후 연결되는 orders.order_id',
                    MODIFY COLUMN `currency` VARCHAR(10) NOT NULL COMMENT '통화 코드 (기본 KRW)',
                    MODIFY COLUMN `total_amount` INT NOT NULL COMMENT '총 결제 금액 (포인트+일반결제+펀딩지원금)',
                    MODIFY COLUMN `point_amount` INT NOT NULL COMMENT '포인트 결제 금액',
                    MODIFY COLUMN `pg_amount` INT NOT NULL COMMENT '일반결제(Mock PG) 금액',
                    MODIFY COLUMN `funding_supported_amount` INT NOT NULL COMMENT '펀딩 지원금으로 상계된 금액',
                    MODIFY COLUMN `status` VARCHAR(40) NOT NULL COMMENT '결제 상태머신 현재 상태',
                    MODIFY COLUMN `pg_provider` VARCHAR(40) NULL COMMENT 'PG 제공자 이름 (예: MOCK-PG)',
                    MODIFY COLUMN `pg_transaction_id` VARCHAR(120) NULL COMMENT 'PG 승인/거래 식별자',
                    MODIFY COLUMN `failure_code` VARCHAR(50) NULL COMMENT '실패 코드',
                    MODIFY COLUMN `failure_message` VARCHAR(255) NULL COMMENT '실패 상세 메시지',
                    MODIFY COLUMN `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '레코드 생성 시각',
                    MODIFY COLUMN `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '레코드 수정 시각'
                """.formatted(paymentIntentTable));

        jdbcTemplate.execute("""
                ALTER TABLE %s
                COMMENT = '결제 단계별 시도 이력 테이블 (append-only)'
                """.formatted(paymentAttemptTable));
        jdbcTemplate.execute("""
                ALTER TABLE %s
                    MODIFY COLUMN `payment_attempt_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '결제 시도 이력 PK',
                    MODIFY COLUMN `payment_intent_id` BIGINT NOT NULL COMMENT 'payment_intent.payment_intent_id FK 성격',
                    MODIFY COLUMN `attempt_no` INT NOT NULL COMMENT '동일 intent 내 시도 순번 (1부터 증가)',
                    MODIFY COLUMN `stage` VARCHAR(40) NOT NULL COMMENT '상태머신 단계명 (INTENT_CREATED, PG_REQUESTED ...)',
                    MODIFY COLUMN `stage_status` VARCHAR(20) NOT NULL COMMENT '단계 처리 결과 (SUCCESS/FAILED)',
                    MODIFY COLUMN `provider` VARCHAR(40) NULL COMMENT '처리한 제공자 (예: MOCK-PG)',
                    MODIFY COLUMN `provider_transaction_id` VARCHAR(120) NULL COMMENT '제공자 거래 ID',
                    MODIFY COLUMN `error_code` VARCHAR(50) NULL COMMENT '실패 코드',
                    MODIFY COLUMN `error_message` VARCHAR(255) NULL COMMENT '실패 메시지',
                    MODIFY COLUMN `requested_amount` INT NOT NULL COMMENT '해당 단계에서 처리하려는 금액',
                    MODIFY COLUMN `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '시도 기록 생성 시각'
                """.formatted(paymentAttemptTable));
    }

    private String validateSchemaName(String name) {
        if (name == null || !SCHEMA_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid payment schema name: " + name);
        }
        return name;
    }
}
