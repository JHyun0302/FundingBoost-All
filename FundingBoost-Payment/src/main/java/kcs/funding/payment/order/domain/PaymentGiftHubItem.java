package kcs.funding.payment.order.domain;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import kcs.funding.payment.common.BaseTimeEntity;
import kcs.funding.payment.friend.domain.PaymentMember;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "giftHub_item", catalog = "fundingboost")
public class PaymentGiftHubItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "giftHub_item_id")
    private Long giftHubItemId;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "item_id", insertable = false, updatable = false)
    private Long itemReferenceId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PaymentMember member;

    @Column(name = "option_name", length = 500)
    private String optionName;
}
