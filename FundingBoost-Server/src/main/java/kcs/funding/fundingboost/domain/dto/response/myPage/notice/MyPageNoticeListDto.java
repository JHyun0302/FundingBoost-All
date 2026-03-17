package kcs.funding.fundingboost.domain.dto.response.myPage.notice;

import java.util.List;
import lombok.Builder;

@Builder
public record MyPageNoticeListDto(
        List<MyPageNoticeDto> notices
) {
    public static MyPageNoticeListDto fromEntity(List<MyPageNoticeDto> notices) {
        return MyPageNoticeListDto.builder()
                .notices(notices)
                .build();
    }
}

