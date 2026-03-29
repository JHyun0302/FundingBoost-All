package kcs.funding.payment.friend.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentContributorRepository extends JpaRepository<PaymentContributor, Long> {
}
