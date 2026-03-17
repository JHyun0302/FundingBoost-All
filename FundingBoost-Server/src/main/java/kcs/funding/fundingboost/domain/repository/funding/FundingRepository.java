package kcs.funding.fundingboost.domain.repository.funding;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kcs.funding.fundingboost.domain.entity.Funding;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FundingRepository extends JpaRepository<Funding, Long>, FundingRepositoryCustom {
    long countByFundingStatus(boolean fundingStatus);

    @Query("select f from Funding f" +
            " join fetch f.member m" +
            " where m.memberId = :memberId and " +
            " f.fundingStatus = :status")
    Optional<Funding> findByMemberIdAndStatus(@Param("memberId") Long memberId, @Param("status") boolean status);

    @Query("select f from Funding f" +
            " join fetch f.member m" +
            " where f.fundingId = :fundingId")
    Funding findMemberById(@Param("fundingId") Long fundingId);

    @Query("select f from Funding f" +
            " join fetch f.fundingItems fi" +
            " join fetch fi.item i" +
            " where f.fundingId = :fundingId")
    Funding findFundingById(@Param("fundingId") Long fundingId);

    @Query("select f from Funding f " +
            "join fetch f.member m " +
            "where f.fundingStatus = true " +
            "and f.deadline between :from and :to " +
            "order by f.deadline asc")
    List<Funding> findExpiringFundings(@Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to,
                                       Pageable pageable);
}
