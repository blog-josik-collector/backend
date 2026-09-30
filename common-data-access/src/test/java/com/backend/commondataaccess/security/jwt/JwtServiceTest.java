package com.backend.commondataaccess.security.jwt;

import com.backend.commondataaccess.exception.UnauthorizedException;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.UserAuthentication;
import com.backend.commondataaccess.persistence.user.enums.LoginProvider;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.commondataaccess.security.jwt.JwtService.Claims;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("JwtService 테스트")
class JwtServiceTest {

    private static final String ISSUER = "test-issuer";
    private static final String CLIENT_SECRET = Base64.getEncoder().encodeToString(
            "test-client-secret-test-client-secret-test-client-secret-012345".getBytes(StandardCharsets.UTF_8));

    private JwtService jwtService;
    private UserAuthentication userAuthentication;

    @BeforeEach
    void init() {
        jwtService = new JwtService(ISSUER, CLIENT_SECRET, 3600);

        User user = User.builder()
                        .id(UUID.randomUUID())
                        .userType(UserType.USER)
                        .nickname("test_nickname")
                        .build();
        userAuthentication = UserAuthentication.builder()
                                               .id(UUID.randomUUID())
                                               .user(user)
                                               .loginProvider(LoginProvider.LOCAL)
                                               .identifier("login")
                                               .credential("pw")
                                               .build();
    }

    @DisplayName("createToken / verify")
    @Nested
    class CreateTokenTest {

        @Test
        void 발급한_토큰을_검증하면_동일한_클레임을_돌려준다() {
            String token = jwtService.createToken(userAuthentication, new String[]{UserType.USER.name()});

            Claims claims = jwtService.verify(token);

            Assertions.assertThat(claims.getAuthenticationId()).isEqualTo(userAuthentication.id());
            Assertions.assertThat(claims.getUserId()).isEqualTo(userAuthentication.user().id());
            Assertions.assertThat(claims.getNickname()).isEqualTo("test_nickname");
            Assertions.assertThat(claims.getRoles()).containsExactly(UserType.USER.name());
            Assertions.assertThat(claims.exp() - claims.iat()).isEqualTo(3600 * 1_000L);
        }

        @Test
        void expirySeconds가_0이면_만료시각_없이_발급한다() {
            JwtService noExpiryService = new JwtService(ISSUER, CLIENT_SECRET, 0);
            String token = noExpiryService.createToken(userAuthentication, new String[]{UserType.USER.name()});

            Claims claims = noExpiryService.verify(token);

            Assertions.assertThat(claims.iat()).isPositive();
            Assertions.assertThat(claims.exp()).isEqualTo(-1);
        }
    }

    @DisplayName("createRefreshToken")
    @Nested
    class CreateRefreshTokenTest {

        @Test
        void 기존_토큰의_클레임으로_새_토큰을_발급한다() {
            String token = jwtService.createToken(userAuthentication, new String[]{UserType.ADMIN.name()});

            String refreshToken = jwtService.createRefreshToken(token);
            Claims claims = jwtService.verify(refreshToken);

            Assertions.assertThat(claims.getAuthenticationId()).isEqualTo(userAuthentication.id());
            Assertions.assertThat(claims.getUserId()).isEqualTo(userAuthentication.user().id());
            Assertions.assertThat(claims.getRoles()).containsExactly(UserType.ADMIN.name());
        }

        @Test
        void 유효하지_않은_토큰이면_UnauthorizedException을_던진다() {
            Assertions.assertThatThrownBy(() -> jwtService.createRefreshToken("invalid.token.value"))
                      .isInstanceOf(UnauthorizedException.class);
        }
    }

    @DisplayName("verify 실패")
    @Nested
    class VerifyFailTest {

        @Test
        void 형식이_잘못된_토큰이면_UnauthorizedException을_던진다() {
            Assertions.assertThatThrownBy(() -> jwtService.verify("invalid.token.value"))
                      .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        void issuer가_다르면_UnauthorizedException을_던진다() {
            String token = new JwtService("other-issuer", CLIENT_SECRET, 3600)
                    .createToken(userAuthentication, new String[]{UserType.USER.name()});

            Assertions.assertThatThrownBy(() -> jwtService.verify(token))
                      .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        void 만료된_토큰이면_UnauthorizedException을_던진다() {
            String token = baseBuilder()
                    .claim("authenticationId", UUID.randomUUID().toString())
                    .claim("userId", UUID.randomUUID().toString())
                    .claim("roles", List.of("USER"))
                    .expiration(new Date(System.currentTimeMillis() - 60_000L))
                    .signWith(jwtService.getKey())
                    .compact();

            Assertions.assertThatThrownBy(() -> jwtService.verify(token))
                      .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        void authenticationId가_없으면_UnauthorizedException을_던진다() {
            String token = baseBuilder()
                    .claim("userId", UUID.randomUUID().toString())
                    .claim("roles", List.of("USER"))
                    .signWith(jwtService.getKey())
                    .compact();

            Assertions.assertThatThrownBy(() -> jwtService.verify(token))
                      .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        void userId가_UUID_형식이_아니면_UnauthorizedException을_던진다() {
            String token = baseBuilder()
                    .claim("authenticationId", UUID.randomUUID().toString())
                    .claim("userId", "not-uuid")
                    .claim("roles", List.of("USER"))
                    .signWith(jwtService.getKey())
                    .compact();

            Assertions.assertThatThrownBy(() -> jwtService.verify(token))
                      .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        void roles가_비어있으면_UnauthorizedException을_던진다() {
            String token = baseBuilder()
                    .claim("authenticationId", UUID.randomUUID().toString())
                    .claim("userId", UUID.randomUUID().toString())
                    .claim("roles", List.of())
                    .signWith(jwtService.getKey())
                    .compact();

            Assertions.assertThatThrownBy(() -> jwtService.verify(token))
                      .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        void roles가_목록이_아니면_UnauthorizedException을_던진다() {
            String token = baseBuilder()
                    .claim("authenticationId", UUID.randomUUID().toString())
                    .claim("userId", UUID.randomUUID().toString())
                    .claim("roles", "USER")
                    .signWith(jwtService.getKey())
                    .compact();

            Assertions.assertThatThrownBy(() -> jwtService.verify(token))
                      .isInstanceOf(UnauthorizedException.class);
        }

        private io.jsonwebtoken.JwtBuilder baseBuilder() {
            return Jwts.builder()
                       .issuer(ISSUER)
                       .issuedAt(new Date());
        }
    }

    @Test
    void Claims_of로_만든_클레임은_iat와_exp가_없다() {
        Claims claims = Claims.of(userAuthentication, new String[]{UserType.USER.name()});

        Assertions.assertThat(claims.iat()).isEqualTo(-1);
        Assertions.assertThat(claims.exp()).isEqualTo(-1);
    }
}
