package kcs.funding.payment.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentDeliveryRepository extends JpaRepository<PaymentDelivery, Long> {
}
