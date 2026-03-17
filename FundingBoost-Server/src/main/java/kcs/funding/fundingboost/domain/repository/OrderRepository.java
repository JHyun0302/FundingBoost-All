package kcs.funding.fundingboost.domain.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import kcs.funding.fundingboost.domain.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByDelivery_DeliveryId(Long deliveryId);

    Optional<Order> findByPaymentIntentKey(String paymentIntentKey);

    long countByCreatedDateBetween(LocalDateTime from, LocalDateTime to);

    @Query("select coalesce(sum(o.totalPrice), 0) from Order o")
    Long sumTotalPrice();

    @Query("select coalesce(sum(o.totalPrice), 0) from Order o " +
            "where o.createdDate between :from and :to")
    Long sumTotalPriceByCreatedDateBetween(@Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);
}
