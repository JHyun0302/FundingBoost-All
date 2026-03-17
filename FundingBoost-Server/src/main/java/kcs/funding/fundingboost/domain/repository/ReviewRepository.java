package kcs.funding.fundingboost.domain.repository;

import java.util.List;
import kcs.funding.fundingboost.domain.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("select r from Review r " +
            "join fetch r.item i " +
            "join fetch r.member m " +
            "where m.memberId = :memberId " +
            "order by r.reviewId desc")
    List<Review> findAllByMemberIdOrderByReviewIdDesc(@Param("memberId") Long memberId);
}
