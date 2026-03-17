package kcs.funding.fundingboost.domain.security.controller;

import java.io.IOException;
import kcs.funding.fundingboost.domain.dto.global.ResponseDto;
import kcs.funding.fundingboost.domain.dto.response.login.JwtDto;
import kcs.funding.fundingboost.domain.exception.CommonException;
import kcs.funding.fundingboost.domain.exception.ErrorCode;
import kcs.funding.fundingboost.domain.security.service.KaKaoLoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class OAuth2AuthenticationController {
    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirectUri;

    private final KaKaoLoginService kaKaoLoginService;

    /**
     * kakao authorization url 반환
     */
    @GetMapping("/login/oauth")
    public ResponseEntity<String> kakaoLogin() {

        String oauth2_url =
                "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=" + clientId + "&redirect_uri="
                        + redirectUri + "&scope=profile_nickname,"
                        + "profile_image,"
                        + "account_email,"
                        + "friends,"
                        + "talk_message";
        return ResponseEntity.ok().body(oauth2_url);
    }

    @GetMapping("/login/oauth2/code/kakao")
    public ResponseDto<JwtDto> kakaoLoginCallback(@RequestParam("code") String code) {
        //클라이언트 서버에서 보내온 코드로 사용자 정보 확인
        JwtDto jwtDto = null;
        try {
            String accessToken = kaKaoLoginService.getAccessTokenFromKakao(clientId, code);
            log.info("Access token: {}", accessToken);
            jwtDto = kaKaoLoginService.getJwtDto(accessToken);
        } catch (IOException e) {
            throw new CommonException(ErrorCode.INVALID_ACCESS_URL);
        }

        return ResponseDto.ok(jwtDto);
    }

}
