package kcs.funding.payment.friend.domain;

import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import kcs.funding.payment.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "funding", catalog = "fundingboost")
public class PaymentFunding extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "funding_id")
    private Long fundingId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PaymentMember member;

    @OneToMany(mappedBy = "funding")
    private List<PaymentFundingItem> fundingItems = new ArrayList<>();

    @NotNull
    @Column(name = "total_price")
    private int totalPrice;

    @NotNull
    @Column(name = "collect_price")
    private int collectPrice;

    @NotNull
    @Column(name = "deadline")
    private LocalDateTime deadline;

    @NotNull
    @Column(name = "funding_status")
    private boolean fundingStatus;

    public void fund(int fundedPoint) {
        this.collectPrice += fundedPoint;
    }

    public void finish() {
        this.fundingStatus = false;
    }
}
