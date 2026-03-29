package kcs.funding.payment.friend.domain;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentFriendPayBarcodeTokenRepository extends JpaRepository<PaymentFriendPayBarcodeToken, Long> {

    Optional<PaymentFriendPayBarcodeToken> findByBarcodeToken(String barcodeToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PaymentFriendPayBarcodeToken t join fetch t.member join fetch t.funding f join fetch f.member where t.barcodeToken = :barcodeToken")
    Optional<PaymentFriendPayBarcodeToken> findByBarcodeTokenForUpdate(@Param("barcodeToken") String barcodeToken);

    List<PaymentFriendPayBarcodeToken> findAllByMemberMemberIdAndFundingFundingIdAndTokenStatus(
            Long memberId,
            Long fundingId,
            PaymentFriendPayBarcodeTokenStatus tokenStatus
    );
}
