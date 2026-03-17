package kcs.funding.fundingboost.domain.repository.orderItem;

import kcs.funding.fundingboost.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>, OrderItemRepositoryCustom {

    @Query("select oi from OrderItem oi" +
            " join fetch oi.item i" +
            " join fetch oi.order o" +
            " join fetch o.member m" +
            " join fetch o.delivery d" +
            " where oi.Id = :orderItemId")
    OrderItem findOrderHistoryDetailById(@Param("orderItemId") Long orderItemId);
}
