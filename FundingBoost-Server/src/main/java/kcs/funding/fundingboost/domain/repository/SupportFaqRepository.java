package kcs.funding.fundingboost.domain.repository;

import java.util.List;
import kcs.funding.fundingboost.domain.entity.SupportFaq;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportFaqRepository extends JpaRepository<SupportFaq, Long> {
    List<SupportFaq> findAllByOrderBySortOrderAscCreatedDateDesc();
}

