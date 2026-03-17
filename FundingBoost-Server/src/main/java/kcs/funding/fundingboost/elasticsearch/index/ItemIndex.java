package kcs.funding.fundingboost.elasticsearch.index;

import kcs.funding.fundingboost.domain.entity.Item;
import kcs.funding.fundingboost.domain.utils.ImageUrlNormalizer;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Document(indexName = "item")
public class ItemIndex {

    @Id
    private Long itemId;

    @Field(name = "item_name", type = FieldType.Text)
    private String itemName;

    @Field(name = "item_price", type = FieldType.Integer)
    private int itemPrice;

    @Field(name = "item_image_url", type = FieldType.Text)
    private String itemImageUrl;

    @Field(name = "brand_name", type = FieldType.Text)
    private String brandName;

    @Field(name = "category", type = FieldType.Keyword)
    private String category;

    @Field(name = "option_name", type = FieldType.Text)
    private String optionName;

    public String getItemImageUrl() {
        return ImageUrlNormalizer.normalize(itemImageUrl);
    }

    public static ItemIndex fromEntity(Item item) {
        ItemIndex itemIndex = new ItemIndex();
        itemIndex.itemId = item.getItemId();
        itemIndex.itemName = item.getItemName();
        itemIndex.itemPrice = item.getItemPrice();
        itemIndex.itemImageUrl = item.getItemImageUrl();
        itemIndex.brandName = item.getBrandName();
        itemIndex.category = item.getCategory();
        itemIndex.optionName = item.getOptionName();
        return itemIndex;
    }
}
