package kcs.funding.payment.friend.domain;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentFundingRepository extends JpaRepository<PaymentFunding, Long> {

    @Query("select f from PaymentFunding f join fetch f.member where f.fundingId = :fundingId")
    Optional<PaymentFunding> findByIdWithMember(@Param("fundingId") Long fundingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from PaymentFunding f join fetch f.member where f.fundingId = :fundingId")
    Optional<PaymentFunding> findByIdWithMemberForUpdate(@Param("fundingId") Long fundingId);
}
