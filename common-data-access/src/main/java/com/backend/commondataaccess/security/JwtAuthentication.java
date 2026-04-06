package com.backend.commondataaccess.security;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.security.jwt.Jwt;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * '인증된 사용자' 의미
 */
@Getter
@Builder
@RequiredArgsConstructor
public class JwtAuthentication {

    private final UUID id;
    private final String userId;
    private final String nickname;
    private final String[] roles;

    public static JwtAuthentication from(User user) {
        return JwtAuthentication.builder()
                                .id(user.id())
                                .userId(user.userId())
                                .nickname(user.nickname())
                                .roles(new String[]{user.userType().name()})
                                .build();
    }

    public static JwtAuthentication from(Jwt.Claims claims) {
        return JwtAuthentication.builder()
                                .id(claims.getId())
                                .userId(claims.getUserId())
                                .nickname(claims.getNickname())
                                .roles(claims.getRoles())
                                .build();
    }
}
