package kcs.funding.fundingboost.payment.api;

import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_LOGIN_USER;

import kcs.funding.fundingboost.domain.dto.global.ResponseDto;
import kcs.funding.fundingboost.domain.exception.CommonException;
import kcs.funding.fundingboost.domain.security.resolver.Login;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/payment/auth")
public class InternalPaymentAuthController {

    @GetMapping("/me")
    public ResponseDto<PaymentAuthMemberDto> resolveAuthenticatedMember(
            @Login Long memberId
    ) {
        if (memberId == null) {
            throw new CommonException(NOT_FOUND_LOGIN_USER);
        }
        return ResponseDto.ok(new PaymentAuthMemberDto(memberId));
    }
}
