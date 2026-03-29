package kcs.funding.payment.friend.domain;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentFundingItemRepository extends JpaRepository<PaymentFundingItem, Long> {

    @Query("select fi from PaymentFundingItem fi where fi.funding.fundingId = :fundingId order by fi.itemSequence asc")
    List<PaymentFundingItem> findAllByFundingIdOrderByItemSequence(@Param("fundingId") Long fundingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select fi from PaymentFundingItem fi
            join fetch fi.funding f
            join fetch f.member
            where fi.fundingItemId = :fundingItemId
            """)
    Optional<PaymentFundingItem> findFundingItemByFundingItemId(@Param("fundingItemId") Long fundingItemId);
}
