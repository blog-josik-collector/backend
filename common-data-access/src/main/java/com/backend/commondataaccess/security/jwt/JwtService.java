package com.backend.commondataaccess.security.jwt;

import com.backend.commondataaccess.persistence.user.UserAuthentication;
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

/**
 * 역할: JWT 자체에 대한 생성/검증/파싱/리프레시를 담당하는 컴포넌트. <p> 입력/출력 <p> - 입력: JWT 문자열, (발급 시) User + roles <p> - 출력: access token 문자열, refresh token 문자열, Claims <p> 책임 <p> - 서명키/issuer/expiry 설정 관리 <p> - 토큰
 * 생성(createToken, createRefreshToken) <p> - 토큰 검증 및 클레임 파싱(verify) <p> - “유효성 여부만” 확인하는 헬퍼(isValidateToken) 제공 <p> 비책임(두면 헷갈리는 영역) <p> - Spring Security의 Authentication, GrantedAuthority 생성/조립은 하지
 * 않음(그건 Converter 책임)
 */
@Getter
@Component
public class JwtService {

    private final String issuer;
    private final int expirySeconds;
    private final SecretKey key; // Key 대신 SecretKey 타입 권장
    private final JwtParser jwtParser;

    public JwtService(@Value("${jwt.issuer}") String issuer,
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

    public String createToken(UserAuthentication userAuthentication, String[] roles) {
        Claims claims = Claims.of(userAuthentication, roles);
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
                .claim("authenticationId", claims.authenticationId)
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

    /**
     * 역할: JWT payload(클레임)를 서비스 공통 포맷으로 담는 DTO. <p> 책임 <p> - JJWT의 claims를 컨트롤러/서비스에서 꺼내 쓰는 필드(id, userId, nickname, roles, iat, exp)로 매핑 <p> - 발급용 클레임 생성(of(User, roles)) 제공 <p> 주의점(주석으로 명확히 적으면 좋은
     * 것) <p> - 이 클래스의 필드가 사실상 서비스 간 계약(contract) 이라, 필드 변경은 토큰 호환성에 영향이 있음
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Claims {

        private UUID authenticationId;
        private UUID userId;
        private String nickname;
        private String[] roles;
        private Date iat;
        private Date exp;

        // JJWT의 JpaClaims를 우리 객체로 매핑
        @SuppressWarnings("unchecked")
        private Claims(io.jsonwebtoken.Claims jjwtClaims) {
            String authenticationIdStr = jjwtClaims.get("authenticationId", String.class);
            this.authenticationId = (authenticationIdStr != null) ? UUID.fromString(authenticationIdStr) : null;

            String userIdStr = jjwtClaims.get("userId", String.class);
            this.userId = (authenticationIdStr != null) ? UUID.fromString(userIdStr) : null;
            this.nickname = jjwtClaims.get("nickname", String.class);

            Object rolesObj = jjwtClaims.get("roles");
            if (rolesObj instanceof List) {
                this.roles = ((List<String>) rolesObj).toArray(new String[0]);
            }

            this.iat = jjwtClaims.getIssuedAt();
            this.exp = jjwtClaims.getExpiration();
        }

        public static Claims of(UserAuthentication userAuthentication, String[] roles) {
            Claims claims = new Claims();
            claims.authenticationId = userAuthentication.id();
            claims.userId = userAuthentication.user().id();
            claims.nickname = userAuthentication.user().nickname();
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
