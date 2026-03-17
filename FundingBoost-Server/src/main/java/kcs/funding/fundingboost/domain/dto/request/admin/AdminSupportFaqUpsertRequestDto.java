package kcs.funding.fundingboost.domain.dto.request.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminSupportFaqUpsertRequestDto(
        @NotBlank
        @Size(max = 300)
        String question,

        @NotBlank
        @Size(max = 4000)
        String answer,

        @Min(1)
        Integer sortOrder
) {
}
