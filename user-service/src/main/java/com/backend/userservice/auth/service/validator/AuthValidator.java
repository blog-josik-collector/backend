package com.backend.userservice.auth.service.validator;

import com.backend.userservice.auth.service.dto.AuthDto;
import java.util.function.UnaryOperator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthValidator {

    public static UnaryOperator<AuthDto> validateUserId() {
        return authDto -> {
            validateUserId(authDto.userId());
            return authDto;
        };
    }

    public static UnaryOperator<AuthDto> validatePassword() {
        return authDto -> {
            validatePassword(authDto.password());
            return authDto;
        };
    }

    public static void validateUserId(String userId) {
        if (StringUtils.isBlank(userId)) {
            throw new IllegalArgumentException("user_id는 필수 입력값입니다.");
        }
    }

    public static void validatePassword(String password) {
        if (StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("password는 필수 입력값입니다.");
        }
    }
}
