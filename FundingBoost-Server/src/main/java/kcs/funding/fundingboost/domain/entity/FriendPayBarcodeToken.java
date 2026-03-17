package kcs.funding.fundingboost.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
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
        name = "friend_pay_barcode_token",
        indexes = {
                @Index(name = "idx_friend_pay_barcode_token", columnList = "barcode_token", unique = true),
                @Index(name = "idx_friend_pay_barcode_expires_at", columnList = "expires_at")
        }
)
public class FriendPayBarcodeToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "friend_pay_barcode_token_id")
    private Long friendPayBarcodeTokenId;

    @NotNull
    @Column(name = "barcode_token", nullable = false, unique = true, length = 120)
    private String barcodeToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Funding funding;

    @NotNull
    @Column(name = "using_point", nullable = false)
    private int usingPoint;

    @NotNull
    @Column(name = "funding_price", nullable = false)
    private int fundingPrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "token_status", nullable = false, length = 20)
    private FriendPayBarcodeTokenStatus tokenStatus;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public static FriendPayBarcodeToken createToken(
            String barcodeToken,
            Member member,
            Funding funding,
            int usingPoint,
            int fundingPrice,
            LocalDateTime expiresAt
    ) {
        FriendPayBarcodeToken token = new FriendPayBarcodeToken();
        token.barcodeToken = barcodeToken;
        token.member = member;
        token.funding = funding;
        token.usingPoint = usingPoint;
        token.fundingPrice = fundingPrice;
        token.tokenStatus = FriendPayBarcodeTokenStatus.PENDING;
        token.expiresAt = expiresAt;
        return token;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    public void markUsed(LocalDateTime now) {
        this.tokenStatus = FriendPayBarcodeTokenStatus.USED;
        this.usedAt = now;
    }

    public void markExpired() {
        if (this.tokenStatus == FriendPayBarcodeTokenStatus.PENDING) {
            this.tokenStatus = FriendPayBarcodeTokenStatus.EXPIRED;
        }
    }
}

