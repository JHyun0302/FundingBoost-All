package kcs.funding.fundingboost.domain.dto.response.myPage.review;

import java.time.format.DateTimeFormatter;
import kcs.funding.fundingboost.domain.entity.Review;
import lombok.Builder;

@Builder
public record MyReviewItemDto(
        Long reviewId,
        Long itemId,
        String itemName,
        String itemImageUrl,
        String optionName,
        int itemPrice,
        int rating,
        String content,
        String createdDate
) {
    public static MyReviewItemDto fromEntity(Review review) {
        return MyReviewItemDto.builder()
                .reviewId(review.getReviewId())
                .itemId(review.getItem().getItemId())
                .itemName(review.getItem().getItemName())
                .itemImageUrl(review.getItem().getItemImageUrl())
                .optionName(review.getItem().getOptionName())
                .itemPrice(review.getItem().getItemPrice())
                .rating(review.getRating())
                .content(review.getContent())
                .createdDate(review.getCreatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .build();
    }
}
