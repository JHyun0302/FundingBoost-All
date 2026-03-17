package kcs.funding.fundingboost.domain.controller;

import kcs.funding.fundingboost.domain.dto.common.CommonSuccessDto;
import kcs.funding.fundingboost.domain.dto.global.ResponseDto;
import kcs.funding.fundingboost.domain.dto.response.pay.friendFundingPay.FriendPayBarcodeVerifyDto;
import kcs.funding.fundingboost.domain.service.pay.FriendPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/barcode-lab")
public class AdminBarcodeLabController {

    private final FriendPayService friendPayService;

    @GetMapping("/access")
    public ResponseDto<CommonSuccessDto> barcodeLabAccess(Authentication authentication) {
        boolean isAdmin = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return ResponseDto.ok(CommonSuccessDto.fromEntity(isAdmin));
    }

    @GetMapping("/tokens/{token}")
    public ResponseDto<FriendPayBarcodeVerifyDto> verifyBarcodeToken(
            @PathVariable("token") String token
    ) {
        return ResponseDto.ok(friendPayService.verifyBarcodeToken(token));
    }
}
