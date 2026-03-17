package kcs.funding.fundingboost.domain.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminNoticeUpsertRequestDto(
        @NotBlank
        @Size(max = 30)
        String category,

        @NotBlank
        @Size(max = 150)
        String title,

        @NotBlank
        @Size(max = 3000)
        String body
) {
}

