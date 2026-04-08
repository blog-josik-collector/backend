package com.backend.userservice.auth.config;

import static org.apache.commons.lang3.ClassUtils.isAssignable;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.commondataaccess.security.JwtAuthenticationToken;
import com.backend.commondataaccess.security.jwt.JwtService;
import com.backend.commondataaccess.security.jwt.JwtService.Claims;
import com.backend.userservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * 역할: JwtAuthenticationToken.from(userId, password)로 들어온 로그인 요청을 처리해서, 사용자 검증 후 인증 완료 Authentication + JWT(access token)를 생성하는 Provider. <p> 책임 <p> - UserService로 사용자 조회 <p> - 비밀번호 검증(현재는 문자열 비교) <p>
 * - JwtTokenService를 통해 access token 발급 <p> - 인증 완료 JwtAuthenticationToken 생성 후 details에 토큰을 담아 반환 <p> 비책임(두면 헷갈리는 영역) <p> - 요청에서 Bearer 토큰을 읽어 SecurityContext에 넣는 일(공통 필터 책임) <p>
 */
@Component
@RequiredArgsConstructor
public class UserAuthenticationProvider implements AuthenticationProvider {

    private final JwtService jwtService;

    private final UserService userService;

    @Override
    public boolean supports(Class<?> authentication) {
        return isAssignable(JwtAuthenticationToken.class, authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        JwtAuthenticationToken authenticationToken = (JwtAuthenticationToken) authentication;
        return createUserAuthentication(authenticationToken);
    }

    private Authentication createUserAuthentication(JwtAuthenticationToken authentication) {
        try {
            String userId = authentication.getPrincipal().toString();
            String password = authentication.getCredentials().toString();

            User user = userService.getUser(userId);

            if (!StringUtils.equals(password, user.password())) {
                throw new IllegalArgumentException("Invalid password");
            }

            String accessToken = jwtService.createToken(user, new String[]{user.userType().name()});
            Claims verifiedClaims = jwtService.verify(accessToken);

            JwtPrincipal principal = JwtPrincipal.from(verifiedClaims);
            JwtAuthenticationToken jwtAuthenticationToken =
                    JwtAuthenticationToken.of(principal, createAuthorityList(user.userType().name()));

            jwtAuthenticationToken.setDetails(accessToken);

            return jwtAuthenticationToken;
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException(e.getMessage());
        } catch (DataAccessException e) {
            throw new AuthenticationServiceException(e.getMessage(), e);
        }
    }
}
