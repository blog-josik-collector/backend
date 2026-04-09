package com.backend.userservice.auth.service;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.security.JwtAuthenticationToken;
import com.backend.userservice.auth.service.dto.AuthDto;
import com.backend.userservice.auth.service.validator.AuthValidator;
import com.backend.userservice.common.validator.ValidationFlow;
import com.backend.userservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthDto.Response loginWithPassword(AuthDto.PasswordRequest passwordRequest) {
        ValidationFlow.start(passwordRequest)
                      .next(AuthValidator.validateUserId())
                      .next(AuthValidator.validatePassword())
                      .end();

        JwtAuthenticationToken authToken = JwtAuthenticationToken.of(passwordRequest.getUserId(),
                                                                     passwordRequest.getPassword());

        Authentication authenticate = authenticationManager.authenticate(authToken);

        User user = userService.getUserByUserId(passwordRequest.getUserId());
        user.login();

        SecurityContextHolder.getContext().setAuthentication(authenticate);

        return AuthDto.Response.from((String) authenticate.getDetails());
    }

    public AuthDto.Response loginWithGoogle(AuthDto.GoogleRequest googleRequest) {
        ValidationFlow.start(googleRequest)
                      .next(AuthValidator.validateSubject())
                      .end();

        JwtAuthenticationToken authToken = JwtAuthenticationToken.from(googleRequest.getSubject());

        Authentication authenticate = authenticationManager.authenticate(authToken);

        User user = userService.getUserBySubjectId(googleRequest.getSubject());
        user.login();

        SecurityContextHolder.getContext().setAuthentication(authenticate);

        return AuthDto.Response.from((String) authenticate.getDetails());
    }
}
