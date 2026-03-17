package kcs.funding.fundingboost.domain.entity;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import kcs.funding.fundingboost.domain.entity.common.BaseTimeEntity;
import kcs.funding.fundingboost.domain.entity.member.Member;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "giftHub_item")
public class GiftHubItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "giftHub_item_id")
    private Long giftHubItemId;

    @ColumnDefault("1")
    @Column(name = "quantity")
    private int quantity;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "item_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Item item;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @Column(name = "option_name", length = 500)
    private String optionName;

    public static GiftHubItem createGiftHubItem(int quantity, Item item, Member member) {
        return createGiftHubItem(quantity, item, member, item.getOptionName());
    }

    public static GiftHubItem createGiftHubItem(int quantity, Item item, Member member, String optionName) {
        GiftHubItem giftHubItem = new GiftHubItem();
        giftHubItem.quantity = quantity;
        giftHubItem.item = item;
        giftHubItem.member = member;
        giftHubItem.optionName = optionName;
        return giftHubItem;
    }

    public void updateQuantity(int quantity) {
        this.quantity = quantity;
    }
}
