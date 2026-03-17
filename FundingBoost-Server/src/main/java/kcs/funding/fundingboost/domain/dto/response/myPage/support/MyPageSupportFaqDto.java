package kcs.funding.fundingboost.domain.dto.response.myPage.support;

import java.time.LocalDateTime;
import kcs.funding.fundingboost.domain.entity.SupportFaq;
import lombok.Builder;

@Builder
public record MyPageSupportFaqDto(
        Long faqId,
        String question,
        String answer,
        int sortOrder,
        LocalDateTime createdDate,
        LocalDateTime modifiedDate
) {
    public static MyPageSupportFaqDto fromEntity(SupportFaq faq) {
        return MyPageSupportFaqDto.builder()
                .faqId(faq.getFaqId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .sortOrder(faq.getSortOrder())
                .createdDate(faq.getCreatedDate())
                .modifiedDate(faq.getModifiedDate())
                .build();
    }
}

