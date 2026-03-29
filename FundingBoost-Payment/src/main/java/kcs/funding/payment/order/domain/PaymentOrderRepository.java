package kcs.funding.payment.order.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByPaymentIntentKey(String paymentIntentKey);
}
