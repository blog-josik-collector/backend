package com.backend.userservice.auth.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.userservice.auth.service.dto.AuthDto;
import java.util.function.UnaryOperator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthValidator {

    public static UnaryOperator<AuthDto.PasswordRequest> validateUserId() {
        return authDto -> {
            validateUserId(authDto.getLoginId());
            return authDto;
        };
    }

    public static UnaryOperator<AuthDto.PasswordRequest> validatePassword() {
        return authDto -> {
            validatePassword(authDto.getPassword());
            return authDto;
        };
    }

    public static UnaryOperator<AuthDto.GoogleRequest> validateSubject() {
        return authDto -> {
            validateSubject(authDto.getSubject());
            return authDto;
        };
    }

    public static void validateUserId(String userId) {
        if (StringUtils.isBlank(userId)) {
            throw new BadRequestException("user_id는 필수 입력값입니다.");
        }
    }

    public static void validatePassword(String password) {
        if (StringUtils.isBlank(password)) {
            throw new BadRequestException("password는 필수 입력값입니다.");
        }
    }

    public static void validateSubject(String subject) {
        if (StringUtils.isBlank(subject)) {
            throw new BadRequestException("subject는 필수 입력값입니다.");
        }
    }
}
