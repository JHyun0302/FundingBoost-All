package kcs.funding.payment.order.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentGiftHubItemRepository extends JpaRepository<PaymentGiftHubItem, Long> {

    long countByGiftHubItemIdInAndMember_MemberId(List<Long> giftHubItemIds, Long memberId);
}
