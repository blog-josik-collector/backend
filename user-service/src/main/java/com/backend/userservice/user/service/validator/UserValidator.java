package com.backend.userservice.user.service.validator;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.userservice.user.service.dto.UserDto;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserValidator {

    public static UnaryOperator<UserDto> validateId() {
        return userDto -> {
            validateId(userDto.userId());
            return userDto;
        };
    }

    public static UnaryOperator<UserDto> validateLoginId() {
        return userDto -> {
            validateLoginId(userDto.loginId());
            return userDto;
        };
    }

    public static UnaryOperator<UserDto> validateNickname() {
        return userDto -> {
            validateNickname(userDto.nickname());
            return userDto;
        };
    }

    public static UnaryOperator<UserDto> validatePasswordAndPasswordConfirm() {
        return userDto -> {
            validatePassword(userDto.password());
            validatePasswordConfirm(userDto.passwordConfirm());
            return userDto;
        };
    }

    public static void validateId(UUID id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("id는 필수 입력값입니다.");
        }
    }

    public static void validateAuthenticationId(UUID authenticationId) {
        if (ObjectUtils.isEmpty(authenticationId)) {
            throw new IllegalArgumentException("authentication_id는 필수 입력값입니다.");
        }
    }

    public static void validateLoginId(String loginId) {
        if (StringUtils.isBlank(loginId)) {
            throw new IllegalArgumentException("login_id는 필수 입력값입니다.");
        }
    }

    public static void validateSubjectId(String subjectId) {
        if (StringUtils.isBlank(subjectId)) {
            throw new IllegalArgumentException("subjectId는 필수 입력값입니다.");
        }
    }

    public static void validateNickname(String nickname) {
        if (StringUtils.isBlank(nickname)) {
            throw new IllegalArgumentException("nickname는 필수 입력값입니다.");
        }
    }

    public static void validatePassword(String password) {
        if (StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("password는 필수 입력값입니다.");
        }
    }

    public static void validatePasswordConfirm(String passwordConfirm) {
        if (StringUtils.isBlank(passwordConfirm)) {
            throw new IllegalArgumentException("password_confirm은 필수 입력값입니다.");
        }
    }

    public static void validateNewPassword(String newPassword) {
        if (StringUtils.isBlank(newPassword)) {
            throw new IllegalArgumentException("new_password는 필수 입력값입니다.");
        }
    }

    public static void validateAccessToken(String accessToken) {
        if (StringUtils.isBlank(accessToken)) {
            throw new IllegalArgumentException("access_token은 필수 입력값입니다.");
        }
    }

    public static void verifyDuplicateNickname(String nickname, Function<String, Boolean> existsByNickname) {
        validateNickname(nickname);
        if (existsByNickname.apply(nickname)) {
            throw new IllegalArgumentException("이미 존재하는 nickname입니다. nickname: " + nickname);
        }
    }

    public static User getUserOrThrow(UUID id, Function<UUID, Optional<User>> fetchOneById) {
        validateId(id);

        return fetchOneById.apply(id)
                       .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 user입니다. id: " + id));
    }
}
