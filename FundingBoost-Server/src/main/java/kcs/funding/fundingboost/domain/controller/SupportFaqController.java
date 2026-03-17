package kcs.funding.fundingboost.domain.controller;

import jakarta.validation.Valid;
import kcs.funding.fundingboost.domain.dto.common.CommonSuccessDto;
import kcs.funding.fundingboost.domain.dto.global.ResponseDto;
import kcs.funding.fundingboost.domain.dto.request.admin.AdminSupportFaqUpsertRequestDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.support.MyPageSupportFaqDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.support.MyPageSupportFaqListDto;
import kcs.funding.fundingboost.domain.service.SupportFaqService;
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
public class SupportFaqController {

    private final SupportFaqService supportFaqService;

    @GetMapping("/api/v1/support/faqs")
    public ResponseDto<MyPageSupportFaqListDto> getFaqs() {
        return ResponseDto.ok(supportFaqService.getFaqs());
    }

    @PostMapping("/api/v1/admin/support/faqs")
    public ResponseDto<MyPageSupportFaqDto> createFaq(@Valid @RequestBody AdminSupportFaqUpsertRequestDto request) {
        return ResponseDto.created(supportFaqService.createFaq(request));
    }

    @PutMapping("/api/v1/admin/support/faqs/{faqId}")
    public ResponseDto<MyPageSupportFaqDto> updateFaq(
            @PathVariable Long faqId,
            @Valid @RequestBody AdminSupportFaqUpsertRequestDto request
    ) {
        return ResponseDto.ok(supportFaqService.updateFaq(faqId, request));
    }

    @DeleteMapping("/api/v1/admin/support/faqs/{faqId}")
    public ResponseDto<CommonSuccessDto> deleteFaq(@PathVariable Long faqId) {
        return ResponseDto.ok(supportFaqService.deleteFaq(faqId));
    }
}

