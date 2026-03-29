package kcs.funding.payment.friend.domain;

import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import kcs.funding.payment.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "contributor", catalog = "fundingboost")
public class PaymentContributor extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contributor_id")
    private Long contributorId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PaymentMember member;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "funding_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PaymentFunding funding;

    @NotNull
    @Column(name = "contributor_price")
    private int contributorPrice;

    public static PaymentContributor createContributor(int contributorPrice, PaymentMember member, PaymentFunding funding) {
        PaymentContributor contributor = new PaymentContributor();
        contributor.contributorPrice = contributorPrice;
        contributor.member = member;
        contributor.funding = funding;
        funding.fund(contributorPrice);
        return contributor;
    }
}
