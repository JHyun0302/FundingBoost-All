package kcs.funding.payment.order.domain;

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
import jakarta.validation.constraints.NotNull;
import kcs.funding.payment.friend.domain.PaymentCatalogItem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "order_item", catalog = "fundingboost")
public class PaymentOrderItem {

    @Id
    @Column(name = "order_item_id")
    @GeneratedValue(strategy = IDENTITY)
    private Long orderItemId;

    @NotNull
    @Column(name = "quantity")
    private int quantity;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "order_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PaymentOrder order;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "item_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private PaymentCatalogItem item;

    @Column(name = "option_name", length = 500)
    private String optionName;

    public static PaymentOrderItem createOrderItem(PaymentOrder order, PaymentCatalogItem item, int quantity, String optionName) {
        PaymentOrderItem orderItem = new PaymentOrderItem();
        orderItem.order = order;
        orderItem.item = item;
        orderItem.quantity = quantity;
        orderItem.optionName = optionName;
        order.plusTotalPrice(item.getItemPrice() * quantity);
        return orderItem;
    }
}
