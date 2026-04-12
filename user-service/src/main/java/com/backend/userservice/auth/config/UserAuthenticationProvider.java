package com.backend.userservice.auth.config;

import static org.apache.commons.lang3.ClassUtils.isAssignable;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

import com.backend.commondataaccess.persistence.user.UserAuthentication;
import com.backend.commondataaccess.security.JwtAuthenticationToken;
import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.commondataaccess.security.jwt.JwtService;
import com.backend.commondataaccess.security.jwt.JwtService.Claims;
import com.backend.userservice.user.service.UserService;
import com.backend.userservice.userauthentication.service.UserAuthenticationService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 역할: JwtAuthenticationToken.from(loginId, password)로 들어온 로그인 요청을 처리해서, 사용자 검증 후 인증 완료 Authentication + JWT(access token)를 생성하는 Provider. <p> 책임 <p> - UserService로 사용자 조회 <p> - 비밀번호 검증(현재는 문자열 비교)
 * <p> - JwtTokenService를 통해 access token 발급 <p> - 인증 완료 JwtAuthenticationToken 생성 후 details에 토큰을 담아 반환 <p> 비책임(두면 헷갈리는 영역) <p> - 요청에서 Bearer 토큰을 읽어 SecurityContext에 넣는 일(공통 필터 책임) <p>
 */
@Component
@RequiredArgsConstructor
public class UserAuthenticationProvider implements AuthenticationProvider {

    private final JwtService jwtService;
    private final UserService userService;
    private final UserAuthenticationService userAuthenticationService;
    private final PasswordEncoder passwordEncoder;

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
            // 1. Context에 따른 사용자 조회 (OAuth vs Local)
            UserAuthentication userAuthentication = fetchUserAuthentication(authentication);

            // 2. 공통 로직: 토큰 발행 및 Authentication 객체 생성
            return createSuccessAuthentication(userAuthentication);

        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException(e.getMessage());
        } catch (DataAccessException e) {
            throw new AuthenticationServiceException(e.getMessage(), e);
        }
    }

    private UserAuthentication fetchUserAuthentication(JwtAuthenticationToken authentication) {
        String principal = authentication.getPrincipal().toString();
        Object credentials = authentication.getCredentials();

        // OAuth Flow: credentials가 없는 경우
        if (ObjectUtils.isEmpty(credentials)) {
            Optional<UserAuthentication> userAuthentication = userAuthenticationService.findUserAuthentication(principal);
            if (userAuthentication.isEmpty()) {
                userService.create(principal);
                return userAuthenticationService.getUserAuthentication(principal);
            } else {
                return userAuthentication.get();
            }
        }

        // Direct Login Flow
        UserAuthentication userAuthentication = userAuthenticationService.getUserAuthentication(principal);
        String password = credentials.toString();

        // 순서 주의: (평문 비밀번호, DB에 저장된 암호화된 비밀번호)
        if (!passwordEncoder.matches(password, userAuthentication.credential())) {
            throw new IllegalArgumentException("Invalid password");
        }

        return userAuthentication;
    }

    private Authentication createSuccessAuthentication(UserAuthentication userAuthentication) {
        String role = userAuthentication.user().userType().name();
        String accessToken = jwtService.createToken(userAuthentication, new String[]{role});

        // 검증 및 Principal 추출
        Claims verifiedClaims = jwtService.verify(accessToken);
        JwtPrincipal principal = JwtPrincipal.from(verifiedClaims);

        // 인증 객체 생성
        JwtAuthenticationToken successToken =
                JwtAuthenticationToken.of(principal, createAuthorityList(role));

        // Details에 생성된 토큰 저장
        successToken.setDetails(accessToken);

        return successToken;
    }
}
