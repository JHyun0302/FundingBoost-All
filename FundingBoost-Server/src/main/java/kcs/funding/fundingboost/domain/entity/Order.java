package kcs.funding.fundingboost.domain.entity;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import kcs.funding.fundingboost.domain.entity.common.BaseTimeEntity;
import kcs.funding.fundingboost.domain.entity.member.Member;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "orders",
        indexes = {
                @Index(name = "uk_orders_payment_intent_key", columnList = "payment_intent_key", unique = true)
        }
)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @NotNull
    private int totalPrice;

    @NotNull
    @Column(name = "point_used_amount")
    private int pointUsedAmount;

    @NotNull
    @Column(name = "direct_paid_amount")
    private int directPaidAmount;

    @NotNull
    @Column(name = "funding_supported_amount")
    private int fundingSupportedAmount;

    @Column(name = "source_funding_id")
    private Long sourceFundingId;

    @Column(name = "payment_intent_key", length = 80)
    private String paymentIntentKey;


    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "delivery_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Delivery delivery;


    public static Order createOrder(Member member, Delivery delivery) {
        Order order = new Order();
        order.totalPrice = 0;
        order.pointUsedAmount = 0;
        order.directPaidAmount = 0;
        order.fundingSupportedAmount = 0;
        order.sourceFundingId = null;
        order.paymentIntentKey = null;
        order.member = member;
        order.delivery = delivery;
        return order;
    }

    public void plusTotalPrice(int plusPrice) {
        totalPrice += plusPrice;
    }

    public void applyPaymentBreakdown(int pointUsedAmount, int directPaidAmount, int fundingSupportedAmount,
                                      Long sourceFundingId) {
        this.pointUsedAmount = Math.max(pointUsedAmount, 0);
        this.directPaidAmount = Math.max(directPaidAmount, 0);
        this.fundingSupportedAmount = Math.max(fundingSupportedAmount, 0);
        this.sourceFundingId = sourceFundingId;
    }

    public void linkPaymentIntentKey(String paymentIntentKey) {
        this.paymentIntentKey = paymentIntentKey;
    }
}
