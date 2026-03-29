package kcs.funding.payment.friend.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCatalogItemRepository extends JpaRepository<PaymentCatalogItem, Long> {

    List<PaymentCatalogItem> findAllByItemIdIn(Collection<Long> itemIds);
}
