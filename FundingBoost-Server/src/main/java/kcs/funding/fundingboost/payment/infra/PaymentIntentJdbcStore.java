package kcs.funding.fundingboost.payment.infra;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import kcs.funding.fundingboost.payment.application.PaymentAttempt;
import kcs.funding.fundingboost.payment.application.PaymentIntentStore;
import kcs.funding.fundingboost.payment.domain.PaymentIntent;
import kcs.funding.fundingboost.payment.domain.PaymentIntentStatus;
import kcs.funding.fundingboost.payment.domain.PaymentIntentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentIntentJdbcStore implements PaymentIntentStore {

    private static final Pattern SCHEMA_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final String paymentIntentTable;
    private final String paymentAttemptTable;

    public PaymentIntentJdbcStore(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            @Value("${app.payment.schema-name:payment}") String schemaName
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        String safeSchemaName = validateSchemaName(schemaName);
        this.paymentIntentTable = "`" + safeSchemaName + "`.`payment_intent`";
        this.paymentAttemptTable = "`" + safeSchemaName + "`.`payment_attempt`";
    }

    @Override
    public void save(PaymentIntent paymentIntent) {
        String sql = """
                INSERT INTO %s (
                    intent_key, member_id, intent_type, reference_id, order_id,
                    currency, total_amount, point_amount, pg_amount, funding_supported_amount,
                    status, pg_provider, pg_transaction_id, failure_code, failure_message
                ) VALUES (
                    :intentKey, :memberId, :intentType, :referenceId, :orderId,
                    :currency, :totalAmount, :pointAmount, :pgAmount, :fundingSupportedAmount,
                    :status, :pgProvider, :pgTransactionId, :failureCode, :failureMessage
                )
                """.formatted(paymentIntentTable);

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("intentKey", paymentIntent.getIntentKey())
                .addValue("memberId", paymentIntent.getMemberId())
                .addValue("intentType", paymentIntent.getIntentType().name())
                .addValue("referenceId", paymentIntent.getReferenceId())
                .addValue("orderId", paymentIntent.getOrderId())
                .addValue("currency", paymentIntent.getCurrency())
                .addValue("totalAmount", paymentIntent.getTotalAmount())
                .addValue("pointAmount", paymentIntent.getPointAmount())
                .addValue("pgAmount", paymentIntent.getPgAmount())
                .addValue("fundingSupportedAmount", paymentIntent.getFundingSupportedAmount())
                .addValue("status", paymentIntent.getStatus().name())
                .addValue("pgProvider", paymentIntent.getPgProvider())
                .addValue("pgTransactionId", paymentIntent.getPgTransactionId())
                .addValue("failureCode", paymentIntent.getFailureCode())
                .addValue("failureMessage", paymentIntent.getFailureMessage());

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update(sql, parameters, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            paymentIntent.assignId(key.longValue());
        }
    }

    @Override
    public void update(PaymentIntent paymentIntent) {
        String sql = """
                UPDATE %s
                   SET order_id = :orderId,
                       status = :status,
                       pg_provider = :pgProvider,
                       pg_transaction_id = :pgTransactionId,
                       failure_code = :failureCode,
                       failure_message = :failureMessage
                 WHERE intent_key = :intentKey
                """.formatted(paymentIntentTable);

        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("orderId", paymentIntent.getOrderId())
                .addValue("status", paymentIntent.getStatus().name())
                .addValue("pgProvider", paymentIntent.getPgProvider())
                .addValue("pgTransactionId", paymentIntent.getPgTransactionId())
                .addValue("failureCode", paymentIntent.getFailureCode())
                .addValue("failureMessage", paymentIntent.getFailureMessage())
                .addValue("intentKey", paymentIntent.getIntentKey()));
    }

    @Override
    public Optional<PaymentIntent> findByIntentKey(String intentKey) {
        String sql = """
                SELECT payment_intent_id, intent_key, member_id, intent_type, reference_id, order_id,
                       currency, total_amount, point_amount, pg_amount, funding_supported_amount,
                       status, pg_provider, pg_transaction_id, failure_code, failure_message,
                       created_at, updated_at
                  FROM %s
                 WHERE intent_key = :intentKey
                 LIMIT 1
                """.formatted(paymentIntentTable);

        return namedParameterJdbcTemplate.query(sql, Map.of("intentKey", intentKey), paymentIntentRowMapper())
                .stream()
                .findFirst();
    }

    @Override
    public void saveAttempt(PaymentAttempt paymentAttempt) {
        String sql = """
                INSERT INTO %s (
                    payment_intent_id, attempt_no, stage, stage_status, provider,
                    provider_transaction_id, error_code, error_message, requested_amount
                ) VALUES (
                    :paymentIntentId, :attemptNo, :stage, :stageStatus, :provider,
                    :providerTransactionId, :errorCode, :errorMessage, :requestedAmount
                )
                """.formatted(paymentAttemptTable);

        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("paymentIntentId", paymentAttempt.paymentIntentId())
                .addValue("attemptNo", paymentAttempt.attemptNo())
                .addValue("stage", paymentAttempt.stage().name())
                .addValue("stageStatus", paymentAttempt.stageStatus())
                .addValue("provider", paymentAttempt.provider())
                .addValue("providerTransactionId", paymentAttempt.providerTransactionId())
                .addValue("errorCode", paymentAttempt.errorCode())
                .addValue("errorMessage", paymentAttempt.errorMessage())
                .addValue("requestedAmount", paymentAttempt.requestedAmount()));
    }

    private RowMapper<PaymentIntent> paymentIntentRowMapper() {
        return (resultSet, rowNum) -> PaymentIntent.rehydrate(
                resultSet.getLong("payment_intent_id"),
                resultSet.getString("intent_key"),
                resultSet.getLong("member_id"),
                PaymentIntentType.valueOf(resultSet.getString("intent_type")),
                getNullableLong(resultSet, "reference_id"),
                getNullableLong(resultSet, "order_id"),
                resultSet.getString("currency"),
                resultSet.getInt("total_amount"),
                resultSet.getInt("point_amount"),
                resultSet.getInt("pg_amount"),
                resultSet.getInt("funding_supported_amount"),
                PaymentIntentStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("pg_provider"),
                resultSet.getString("pg_transaction_id"),
                resultSet.getString("failure_code"),
                resultSet.getString("failure_message"),
                getLocalDateTime(resultSet, "created_at"),
                getLocalDateTime(resultSet, "updated_at")
        );
    }

    private static String validateSchemaName(String schemaName) {
        if (schemaName == null || !SCHEMA_NAME_PATTERN.matcher(schemaName).matches()) {
            throw new IllegalArgumentException("Invalid payment schema name: " + schemaName);
        }
        return schemaName;
    }

    private static Long getNullableLong(ResultSet resultSet, String columnName) throws SQLException {
        long value = resultSet.getLong(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private static LocalDateTime getLocalDateTime(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
