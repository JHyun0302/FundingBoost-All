package kcs.funding.fundingboost.domain.security.entity;

public record KakaoOAuthToken(
        String token_type,
        String access_token,
        Integer expires_in,
        String refresh_token,
        Integer refresh_token_expires_in,
        String scope
) {
}
