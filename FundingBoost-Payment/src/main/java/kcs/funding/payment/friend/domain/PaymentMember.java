package kcs.funding.payment.friend.domain;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import kcs.funding.payment.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member", catalog = "fundingboost")
public class PaymentMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @NotNull
    @Column(name = "nick_name", length = 20)
    private String nickName;

    @NotNull
    @Column(name = "point")
    private int point;

    public void minusPoint(int usingPoint) {
        this.point -= usingPoint;
    }
}
