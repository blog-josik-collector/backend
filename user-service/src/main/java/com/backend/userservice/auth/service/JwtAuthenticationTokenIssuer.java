package com.backend.userservice.auth.service;

import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

import com.backend.commondataaccess.persistence.user.UserAuthentication;
import com.backend.commondataaccess.security.JwtAuthenticationToken;
import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.commondataaccess.security.jwt.JwtService;
import com.backend.commondataaccess.security.jwt.JwtService.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 검증이 끝난 {@link UserAuthentication} 으로 JWT access token 과 인증 완료 {@link Authentication} 을 생성한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationTokenIssuer {

    private final JwtService jwtService;

    public Authentication issue(UserAuthentication userAuthentication) {
        String role = userAuthentication.user().userType().name();
        String accessToken = jwtService.createToken(userAuthentication, new String[]{role});

        Claims verifiedClaims = jwtService.verify(accessToken);
        JwtPrincipal principal = JwtPrincipal.from(verifiedClaims);

        JwtAuthenticationToken successToken =
                JwtAuthenticationToken.of(principal, createAuthorityList(role));
        successToken.setDetails(accessToken);

        return successToken;
    }
}
