package kcs.funding.fundingboost.domain.repository;

import java.util.List;
import java.util.Optional;
import kcs.funding.fundingboost.domain.entity.FriendPayBarcodeToken;
import kcs.funding.fundingboost.domain.entity.FriendPayBarcodeTokenStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FriendPayBarcodeTokenRepository extends JpaRepository<FriendPayBarcodeToken, Long> {
    Optional<FriendPayBarcodeToken> findByBarcodeToken(String barcodeToken);

    List<FriendPayBarcodeToken> findAllByMemberMemberIdAndFundingFundingIdAndTokenStatus(
            Long memberId,
            Long fundingId,
            FriendPayBarcodeTokenStatus tokenStatus
    );

    long countByTokenStatus(FriendPayBarcodeTokenStatus tokenStatus);

    @Query("select t from FriendPayBarcodeToken t " +
            "join fetch t.member m " +
            "join fetch t.funding f " +
            "join fetch f.member fm " +
            "order by t.createdDate desc")
    List<FriendPayBarcodeToken> findRecentTokens(Pageable pageable);
}
