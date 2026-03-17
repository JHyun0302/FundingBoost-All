package kcs.funding.fundingboost.domain.dto.response.myPage.notice;

import java.time.LocalDateTime;
import kcs.funding.fundingboost.domain.entity.Notice;
import lombok.Builder;

@Builder
public record MyPageNoticeDto(
        Long noticeId,
        String category,
        String title,
        String body,
        LocalDateTime createdDate,
        LocalDateTime modifiedDate
) {
    public static MyPageNoticeDto fromEntity(Notice notice) {
        return MyPageNoticeDto.builder()
                .noticeId(notice.getNoticeId())
                .category(notice.getCategory())
                .title(notice.getTitle())
                .body(notice.getBody())
                .createdDate(notice.getCreatedDate())
                .modifiedDate(notice.getModifiedDate())
                .build();
    }
}

