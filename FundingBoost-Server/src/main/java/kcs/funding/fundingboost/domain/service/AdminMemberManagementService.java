package kcs.funding.fundingboost.domain.service;

import static kcs.funding.fundingboost.domain.exception.ErrorCode.BAD_REQUEST_PARAMETER;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_MEMBER;

import kcs.funding.fundingboost.domain.dto.response.admin.AdminMemberPageDto;
import kcs.funding.fundingboost.domain.dto.response.admin.AdminMemberRoleUpdateDto;
import kcs.funding.fundingboost.domain.dto.response.admin.AdminMemberSummaryDto;
import kcs.funding.fundingboost.domain.entity.member.Member;
import kcs.funding.fundingboost.domain.entity.member.MemberRole;
import kcs.funding.fundingboost.domain.exception.CommonException;
import kcs.funding.fundingboost.domain.repository.MemberRepository;
import kcs.funding.fundingboost.domain.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberManagementService {

    private static final int MAX_PAGE_SIZE = 50;

    private final MemberRepository memberRepository;

    public AdminMemberPageDto getMembers(String keyword, MemberRole role, int page, int size) {
        int validPage = Math.max(page, 0);
        int validSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<Member> members = memberRepository.searchForAdmin(
                keyword == null ? "" : keyword.trim(),
                role,
                PageRequest.of(validPage, validSize, Sort.by(Sort.Order.desc("createdDate"), Sort.Order.desc("memberId")))
        );

        return AdminMemberPageDto.from(
                members.getContent().stream()
                        .map(this::toSummaryDto)
                        .toList(),
                members.getNumber(),
                members.getSize(),
                members.getTotalElements(),
                members.getTotalPages(),
                members.hasNext()
        );
    }

    @Transactional
    public AdminMemberRoleUpdateDto updateMemberRole(
            Long targetMemberId,
            MemberRole targetRole,
            Authentication authentication
    ) {
        if (targetRole == null) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }

        Member targetMember = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));

        Long actorMemberId = resolveActorMemberId(authentication);
        if (actorMemberId != null
                && actorMemberId.equals(targetMemberId)
                && targetRole == MemberRole.ROLE_USER) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }

        if (targetMember.getMemberRole() == MemberRole.ROLE_ADMIN
                && targetRole == MemberRole.ROLE_USER
                && memberRepository.countByMemberRole(MemberRole.ROLE_ADMIN) <= 1) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }

        targetMember.changeMemberRole(targetRole);

        return AdminMemberRoleUpdateDto.from(
                targetMember.getMemberId(),
                targetMember.getEmail(),
                targetMember.getNickName(),
                targetMember.getMemberRole().name()
        );
    }

    private AdminMemberSummaryDto toSummaryDto(Member member) {
        String genderName = member.getGender() == null ? "UNKNOWN" : member.getGender().name();
        return AdminMemberSummaryDto.from(
                member.getMemberId(),
                member.getNickName(),
                member.getEmail(),
                member.getMemberRole().name(),
                genderName,
                member.getPoint(),
                member.getCreatedDate()
        );
    }

    private Long resolveActorMemberId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getMemberId();
        }
        return null;
    }
}
