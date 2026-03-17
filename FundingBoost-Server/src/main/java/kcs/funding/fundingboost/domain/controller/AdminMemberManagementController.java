package kcs.funding.fundingboost.domain.controller;

import kcs.funding.fundingboost.domain.dto.global.ResponseDto;
import kcs.funding.fundingboost.domain.dto.request.admin.AdminMemberRoleUpdateRequestDto;
import kcs.funding.fundingboost.domain.dto.response.admin.AdminMemberPageDto;
import kcs.funding.fundingboost.domain.dto.response.admin.AdminMemberRoleUpdateDto;
import kcs.funding.fundingboost.domain.entity.member.MemberRole;
import kcs.funding.fundingboost.domain.service.AdminMemberManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/members")
public class AdminMemberManagementController {

    private final AdminMemberManagementService adminMemberManagementService;

    @GetMapping("")
    public ResponseDto<AdminMemberPageDto> getMembers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) MemberRole role,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size
    ) {
        return ResponseDto.ok(adminMemberManagementService.getMembers(keyword, role, page, size));
    }

    @PatchMapping("/{memberId}/role")
    public ResponseDto<AdminMemberRoleUpdateDto> updateMemberRole(
            @PathVariable("memberId") Long memberId,
            @RequestBody AdminMemberRoleUpdateRequestDto request,
            Authentication authentication
    ) {
        return ResponseDto.ok(
                adminMemberManagementService.updateMemberRole(memberId, request.role(), authentication)
        );
    }
}
