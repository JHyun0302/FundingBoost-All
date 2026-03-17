package kcs.funding.fundingboost.domain.dto.response.myPage.review;

import java.util.List;
import kcs.funding.fundingboost.domain.dto.response.myPage.MyPageMemberDto;
import lombok.Builder;

@Builder
public record MyReviewHistoryDto(
        MyPageMemberDto myPageMemberDto,
        List<MyReviewItemDto> myReviewItemDtoList
) {
    public static MyReviewHistoryDto fromEntity(
            MyPageMemberDto myPageMemberDto,
            List<MyReviewItemDto> myReviewItemDtoList
    ) {
        return MyReviewHistoryDto.builder()
                .myPageMemberDto(myPageMemberDto)
                .myReviewItemDtoList(myReviewItemDtoList)
                .build();
    }
}
