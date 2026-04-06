package com.backend.commondataaccess.security.jwt;

import com.backend.commondataaccess.persistence.user.User;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class Jwt {

    private final String issuer;
    private final int expirySeconds;
    private final SecretKey key; // Key 대신 SecretKey 타입 권장
    private final JwtParser jwtParser;

    public Jwt(@Value("${jwt.issuer}") String issuer,
               @Value("${jwt.client-secret}") String clientSecret,
               @Value("${jwt.expiry-seconds}") int expirySeconds) {

        this.issuer = issuer;
        this.expirySeconds = expirySeconds;
        // JJWT 0.12.x 방식: 평문 비밀번호를 Base64 디코딩하여 키 생성
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(clientSecret));
        this.jwtParser = Jwts.parser()
                             .verifyWith(key)
                             .requireIssuer(issuer)
                             .build();
    }

    public String createToken(User user, String[] roles) {
        Claims claims = Claims.of(user, roles);
        return createNewToken(claims);
    }

    private String createNewToken(Claims claims) {
        Date now = new Date();
        JwtBuilder builder = Jwts.builder()
                                 .issuer(issuer)
                                 .issuedAt(now);

        if (expirySeconds > 0) {
            builder.expiration(new Date(now.getTime() + expirySeconds * 1_000L));
        }

        return builder
                .claim("id", claims.id)
                .claim("userId", claims.userId)
                .claim("nickname", claims.nickname)
                .claim("roles", claims.roles)
                .signWith(key) // 알고리즘은 키 길이에 따라 자동 선택됨 (HS512 등)
                .compact();
    }

    public String createRefreshToken(String token) {
        Claims claims = verify(token);
        claims.eraseIat();
        claims.eraseExp();
        return createNewToken(claims);
    }

    public Claims verify(String token) {
        // JJWT의 파싱 결과를 우리 도메인 모델인 Claims로 변환
        return new Claims(jwtParser.parseSignedClaims(token).getPayload());
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Claims {

        private UUID id;
        private String userId;
        private String nickname;
        private String[] roles;
        private Date iat;
        private Date exp;

        // JJWT의 JpaClaims를 우리 객체로 매핑
        @SuppressWarnings("unchecked")
        private Claims(io.jsonwebtoken.Claims jjwtClaims) {
            String idStr = jjwtClaims.get("id", String.class);
            this.id = (idStr != null) ? UUID.fromString(idStr) : null;
            this.userId = jjwtClaims.get("userId", String.class);
            this.nickname = jjwtClaims.get("nickname", String.class);

            Object rolesObj = jjwtClaims.get("roles");
            if (rolesObj instanceof List) {
                this.roles = ((List<String>) rolesObj).toArray(new String[0]);
            }

            this.iat = jjwtClaims.getIssuedAt();
            this.exp = jjwtClaims.getExpiration();
        }

        public static Claims of(User user, String[] roles) {
            Claims claims = new Claims();
            claims.id = user.id();
            claims.userId = user.userId();
            claims.nickname = user.nickname();
            claims.roles = roles;
            return claims;
        }

        public long iat() {
            return iat != null ? iat.getTime() : -1;
        }

        public long exp() {
            return exp != null ? exp.getTime() : -1;
        }

        public void eraseIat() {
            iat = null;
        }

        public void eraseExp() {
            exp = null;
        }
    }
}
