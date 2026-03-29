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
@Table(name = "delivery", catalog = "fundingboost")
public class PaymentDelivery extends BaseTimeEntity {

    @Id
    @Column(name = "delivery_id")
    @GeneratedValue(strategy = IDENTITY)
    private Long deliveryId;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "phone_number", length = 13)
    private String phoneNumber;

    @Column(name = "customer_name", length = 50)
    private String customerName;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "delivery_memo", length = 200)
    private String deliveryMemo;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PaymentMember member;
}
