package kcs.funding.payment.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOrderItemRepository extends JpaRepository<PaymentOrderItem, Long> {
}
