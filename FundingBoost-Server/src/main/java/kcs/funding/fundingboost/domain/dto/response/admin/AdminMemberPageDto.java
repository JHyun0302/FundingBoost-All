package kcs.funding.fundingboost.domain.dto.response.admin;

import java.util.List;
import lombok.Builder;

@Builder
public record AdminMemberPageDto(
        List<AdminMemberSummaryDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static AdminMemberPageDto from(
            List<AdminMemberSummaryDto> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {
        return AdminMemberPageDto.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(hasNext)
                .build();
    }
}
