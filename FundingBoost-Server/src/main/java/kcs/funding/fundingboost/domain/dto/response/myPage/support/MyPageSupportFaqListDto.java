package kcs.funding.fundingboost.domain.dto.response.myPage.support;

import java.util.List;
import lombok.Builder;

@Builder
public record MyPageSupportFaqListDto(
        List<MyPageSupportFaqDto> faqs
) {
    public static MyPageSupportFaqListDto fromEntity(List<MyPageSupportFaqDto> faqs) {
        return MyPageSupportFaqListDto.builder()
                .faqs(faqs)
                .build();
    }
}

