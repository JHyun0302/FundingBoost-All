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
@Table(name = "funding_item", catalog = "fundingboost")
public class PaymentFundingItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "funding_item_id")
    private Long fundingItemId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "funding_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PaymentFunding funding;

    @Column(name = "item_id", insertable = false, updatable = false)
    private Long itemReferenceId;

    @NotNull
    @Column(name = "item_sequence")
    private int itemSequence;

    @NotNull
    @Column(name = "item_status")
    private boolean itemStatus;

    @NotNull
    @Column(name = "finished_status")
    private boolean finishedStatus;

    public void completeFunding() {
        this.itemStatus = false;
    }

    public void finishFundingItem() {
        this.finishedStatus = false;
    }
}
