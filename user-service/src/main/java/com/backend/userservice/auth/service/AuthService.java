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

    public AuthDto login(AuthDto authDto) {
        ValidationFlow.start(authDto)
                      .next(AuthValidator.validateUserId())
                      .next(AuthValidator.validatePassword())
                      .end();

        JwtAuthenticationToken authToken = JwtAuthenticationToken.from(authDto.userId(),
                                                                       authDto.password());

        Authentication authenticate = authenticationManager.authenticate(authToken);

        User user = userService.getUser(authDto.userId());
        user.login();

        SecurityContextHolder.getContext().setAuthentication(authenticate);

        return AuthDto.from((String) authenticate.getDetails());
    }
}
