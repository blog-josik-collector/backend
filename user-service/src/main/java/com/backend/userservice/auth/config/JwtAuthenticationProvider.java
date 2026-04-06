package com.backend.userservice.auth.config;

import static org.apache.commons.lang3.ClassUtils.isAssignable;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.security.JwtAuthentication;
import com.backend.commondataaccess.security.JwtAuthenticationToken;
import com.backend.commondataaccess.security.jwt.JwtTokenProvider;
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

@Component
@RequiredArgsConstructor
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtTokenProvider jwtTokenProvider;

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

            JwtAuthentication jwtAuthentication = JwtAuthentication.from(user);
            JwtAuthenticationToken jwtAuthenticationToken =
                    JwtAuthenticationToken.of(jwtAuthentication, createAuthorityList(user.userType().name()));

            String jwtToken = jwtTokenProvider.createToken(user, new String[]{user.userType().name()});
            jwtAuthenticationToken.setDetails(jwtToken);

            return jwtAuthenticationToken;
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException(e.getMessage());
        } catch (DataAccessException e) {
            throw new AuthenticationServiceException(e.getMessage(), e);
        }
    }
}
