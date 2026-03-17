package kcs.funding.fundingboost.payment.application;

import static kcs.funding.fundingboost.domain.exception.ErrorCode.BAD_REQUEST_PARAMETER;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.INTERNAL_SERVER_ERROR;

import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import kcs.funding.fundingboost.domain.exception.CommonException;
import kcs.funding.fundingboost.payment.domain.PaymentAttemptStage;
import kcs.funding.fundingboost.payment.domain.PaymentIntent;
import kcs.funding.fundingboost.payment.domain.PaymentIntentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentIntentOrchestrator {

    private final PaymentIntentStore paymentIntentStore;
    private final GeneralPaymentGateway generalPaymentGateway;

    @Transactional
    public PaymentExecutionResult execute(PaymentExecutionCommand command) {
        String intentKey = PaymentIntentKeyResolver.resolveFromIdempotencyKey(command.idempotencyKey())
                .orElseGet(this::generateIntentKey);

        Optional<PaymentIntent> existingIntent = paymentIntentStore.findByIntentKey(intentKey);
        if (existingIntent.isPresent()) {
            PaymentIntent existing = existingIntent.get();
            if (!isSamePaymentRequest(existing, command)) {
                throw new CommonException(BAD_REQUEST_PARAMETER);
            }
            if (existing.getStatus() != PaymentIntentStatus.CAPTURED) {
                throw new CommonException(INTERNAL_SERVER_ERROR);
            }
            return new PaymentExecutionResult(
                    existing.getIntentKey(),
                    existing.getStatus(),
                    existing.getPgProvider(),
                    existing.getPgTransactionId()
            );
        }

        PaymentIntent paymentIntent = PaymentIntent.create(
                intentKey,
                command.memberId(),
                command.intentType(),
                command.referenceId(),
                command.currency(),
                command.totalAmount(),
                command.pointAmount(),
                command.pgAmount(),
                command.fundingSupportedAmount()
        );
        paymentIntentStore.save(paymentIntent);

        int attemptNo = 1;
        paymentIntentStore.saveAttempt(PaymentAttempt.success(
                paymentIntent.getPaymentIntentId(),
                attemptNo++,
                PaymentAttemptStage.INTENT_CREATED,
                null,
                null,
                0
        ));

        if (command.pointAmount() > 0) {
            paymentIntent.transitionTo(PaymentIntentStatus.POINT_RESERVED);
            paymentIntentStore.update(paymentIntent);
            paymentIntentStore.saveAttempt(PaymentAttempt.success(
                    paymentIntent.getPaymentIntentId(),
                    attemptNo++,
                    PaymentAttemptStage.POINT_RESERVED,
                    null,
                    null,
                    command.pointAmount()
            ));
        }

        if (command.pgAmount() > 0) {
            paymentIntent.transitionTo(PaymentIntentStatus.PG_REQUESTED);
            paymentIntentStore.update(paymentIntent);
            paymentIntentStore.saveAttempt(PaymentAttempt.success(
                    paymentIntent.getPaymentIntentId(),
                    attemptNo++,
                    PaymentAttemptStage.PG_REQUESTED,
                    null,
                    null,
                    command.pgAmount()
            ));

            PgAuthorizeResult authorizeResult = generalPaymentGateway.authorize(
                    new PgAuthorizeRequest(
                            paymentIntent.getIntentKey(),
                            command.memberId(),
                            command.pgAmount(),
                            command.currency()
                    )
            );

            if (!authorizeResult.approved()) {
                paymentIntent.fail(authorizeResult.errorCode(), authorizeResult.errorMessage());
                paymentIntentStore.update(paymentIntent);
                paymentIntentStore.saveAttempt(PaymentAttempt.failure(
                        paymentIntent.getPaymentIntentId(),
                        attemptNo,
                        PaymentAttemptStage.PG_AUTHORIZED,
                        authorizeResult.provider(),
                        Optional.ofNullable(authorizeResult.errorCode()).orElse("PG_AUTH_FAILED"),
                        Optional.ofNullable(authorizeResult.errorMessage()).orElse("mock pg authorization failed"),
                        command.pgAmount()
                ));
                throw new CommonException(INTERNAL_SERVER_ERROR);
            }

            paymentIntent.approvePg(authorizeResult.provider(), authorizeResult.transactionId());
            paymentIntentStore.update(paymentIntent);
            paymentIntentStore.saveAttempt(PaymentAttempt.success(
                    paymentIntent.getPaymentIntentId(),
                    attemptNo++,
                    PaymentAttemptStage.PG_AUTHORIZED,
                    authorizeResult.provider(),
                    authorizeResult.transactionId(),
                    command.pgAmount()
            ));
        }

        paymentIntent.transitionTo(PaymentIntentStatus.CAPTURED);
        paymentIntentStore.update(paymentIntent);
        paymentIntentStore.saveAttempt(PaymentAttempt.success(
                paymentIntent.getPaymentIntentId(),
                attemptNo,
                PaymentAttemptStage.CAPTURED,
                paymentIntent.getPgProvider(),
                paymentIntent.getPgTransactionId(),
                command.totalAmount()
        ));

        return new PaymentExecutionResult(
                paymentIntent.getIntentKey(),
                paymentIntent.getStatus(),
                paymentIntent.getPgProvider(),
                paymentIntent.getPgTransactionId()
        );
    }

    @Transactional
    public void attachOrderId(String intentKey, Long orderId) {
        PaymentIntent paymentIntent = paymentIntentStore.findByIntentKey(intentKey)
                .orElseThrow(() -> new CommonException(INTERNAL_SERVER_ERROR));
        paymentIntent.attachOrderId(orderId);
        paymentIntentStore.update(paymentIntent);
    }

    private String generateIntentKey() {
        return "pi_" + UUID.randomUUID().toString().replace("-", "");
    }

    private boolean isSamePaymentRequest(PaymentIntent existing, PaymentExecutionCommand command) {
        return Objects.equals(existing.getMemberId(), command.memberId())
                && existing.getIntentType() == command.intentType()
                && Objects.equals(existing.getReferenceId(), command.referenceId())
                && Objects.equals(existing.getCurrency(), command.currency())
                && existing.getTotalAmount() == command.totalAmount()
                && existing.getPointAmount() == command.pointAmount()
                && existing.getPgAmount() == command.pgAmount()
                && existing.getFundingSupportedAmount() == command.fundingSupportedAmount();
    }
}
