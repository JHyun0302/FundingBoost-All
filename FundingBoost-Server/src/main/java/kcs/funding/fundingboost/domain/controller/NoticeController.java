package kcs.funding.fundingboost.domain.controller;

import jakarta.validation.Valid;
import kcs.funding.fundingboost.domain.dto.common.CommonSuccessDto;
import kcs.funding.fundingboost.domain.dto.global.ResponseDto;
import kcs.funding.fundingboost.domain.dto.request.admin.AdminNoticeUpsertRequestDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.notice.MyPageNoticeDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.notice.MyPageNoticeListDto;
import kcs.funding.fundingboost.domain.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/api/v1/notices")
    public ResponseDto<MyPageNoticeListDto> getNotices() {
        return ResponseDto.ok(noticeService.getNotices());
    }

    @PostMapping("/api/v1/admin/notices")
    public ResponseDto<MyPageNoticeDto> createNotice(@Valid @RequestBody AdminNoticeUpsertRequestDto request) {
        return ResponseDto.created(noticeService.createNotice(request));
    }

    @PutMapping("/api/v1/admin/notices/{noticeId}")
    public ResponseDto<MyPageNoticeDto> updateNotice(
            @PathVariable Long noticeId,
            @Valid @RequestBody AdminNoticeUpsertRequestDto request
    ) {
        return ResponseDto.ok(noticeService.updateNotice(noticeId, request));
    }

    @DeleteMapping("/api/v1/admin/notices/{noticeId}")
    public ResponseDto<CommonSuccessDto> deleteNotice(@PathVariable Long noticeId) {
        return ResponseDto.ok(noticeService.deleteNotice(noticeId));
    }
}

