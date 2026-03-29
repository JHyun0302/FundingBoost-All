package kcs.funding.crawler.catalog.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtMemberResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${catalog.auth.jwt-secret:tmptmptmptmptmptmptmptmptmptmptmptmptmptmptmptmptmptmptmptmptmptmptmptmptmptmptmptmptmptmptmptmp}")
    private String jwtSecret;

    public Optional<Long> resolveMemberId(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return Optional.empty();
        }
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            return Optional.empty();
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(resolveKey(jwtSecret))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (Exception exception) {
            log.debug("failed to resolve memberId from jwt: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private static Key resolveKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception ignored) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
