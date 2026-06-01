package com.backend.userservice.auth.service;

import com.backend.commondataaccess.exception.UnauthorizedException;
import com.backend.commondataaccess.persistence.user.UserAuthentication;
import com.backend.commondataaccess.service.validator.ValidationFlow;
import com.backend.userservice.auth.service.dto.AuthDto;
import com.backend.userservice.auth.service.validator.AuthValidator;
import com.backend.userservice.user.service.UserService;
import com.backend.userservice.userauthentication.service.UserAuthenticationService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAuthenticationService userAuthenticationService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthenticationTokenIssuer jwtAuthenticationTokenIssuer;

    public AuthDto.Response loginWithPassword(AuthDto.PasswordRequest passwordRequest) {
        ValidationFlow.start(passwordRequest)
                      .next(AuthValidator.validateUserId())
                      .next(AuthValidator.validatePassword())
                      .end();

        UserAuthentication userAuthentication =
                userAuthenticationService.getUserAuthentication(passwordRequest.getLoginId());

        if (!passwordEncoder.matches(passwordRequest.getPassword(), userAuthentication.credential())) {
            throw new UnauthorizedException("Invalid password");
        }

        return completeLogin(userAuthentication);
    }

    public AuthDto.Response loginWithGoogle(AuthDto.GoogleRequest googleRequest) {
        ValidationFlow.start(googleRequest)
                      .next(AuthValidator.validateSubject())
                      .end();

        UserAuthentication userAuthentication = resolveGoogleUserAuthentication(googleRequest.getSubject());

        return completeLogin(userAuthentication);
    }

    private UserAuthentication resolveGoogleUserAuthentication(String subject) {
        Optional<UserAuthentication> existing = userAuthenticationService.findUserAuthentication(subject);
        if (existing.isPresent()) {
            return existing.get();
        }

        userService.create(subject);
        return userAuthenticationService.getUserAuthentication(subject);
    }

    private AuthDto.Response completeLogin(UserAuthentication userAuthentication) {
        Authentication authenticate = jwtAuthenticationTokenIssuer.issue(userAuthentication);

        userAuthentication.user().login();
        SecurityContextHolder.getContext().setAuthentication(authenticate);

        return AuthDto.Response.from((String) authenticate.getDetails());
    }
}
