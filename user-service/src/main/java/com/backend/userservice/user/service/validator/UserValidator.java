package com.backend.userservice.user.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.exception.StateConflictException;
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

    private static final String RESERVED_LOGIN_ID_WORD = "admin";
    private static final String RESERVED_NICKNAME_WORD_EN = "admin";
    private static final String RESERVED_NICKNAME_WORD_KO = "어드민";

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

    public static UnaryOperator<UserDto> validateLoginIdNotReserved() {
        return userDto -> {
            validateLoginIdNotReserved(userDto.loginId());
            return userDto;
        };
    }

    public static UnaryOperator<UserDto> validateNicknameNotReserved() {
        return userDto -> {
            validateNicknameNotReserved(userDto.nickname());
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
            throw new BadRequestException("id는 필수 입력값입니다.");
        }
    }

    public static void validateAuthenticationId(UUID authenticationId) {
        if (ObjectUtils.isEmpty(authenticationId)) {
            throw new BadRequestException("authentication_id는 필수 입력값입니다.");
        }
    }

    public static void validateLoginId(String loginId) {
        if (StringUtils.isBlank(loginId)) {
            throw new BadRequestException("login_id는 필수 입력값입니다.");
        }
    }

    public static void validateLoginIdNotReserved(String loginId) {
        if (StringUtils.containsIgnoreCase(loginId, RESERVED_LOGIN_ID_WORD)) {
            throw new BadRequestException("login_id에 'admin'을 포함할 수 없습니다.");
        }
    }

    public static void validateSubjectId(String subjectId) {
        if (StringUtils.isBlank(subjectId)) {
            throw new BadRequestException("subjectId는 필수 입력값입니다.");
        }
    }

    public static void validateNickname(String nickname) {
        if (StringUtils.isBlank(nickname)) {
            throw new BadRequestException("nickname는 필수 입력값입니다.");
        }
    }

    public static void validateNicknameNotReserved(String nickname) {
        if (StringUtils.containsIgnoreCase(nickname, RESERVED_NICKNAME_WORD_EN)
                || StringUtils.contains(nickname, RESERVED_NICKNAME_WORD_KO)) {
            throw new BadRequestException("nickname에 'admin' 또는 '어드민'을 포함할 수 없습니다.");
        }
    }

    public static void validatePassword(String password) {
        if (StringUtils.isBlank(password)) {
            throw new BadRequestException("password는 필수 입력값입니다.");
        }
    }

    public static void validatePasswordConfirm(String passwordConfirm) {
        if (StringUtils.isBlank(passwordConfirm)) {
            throw new BadRequestException("password_confirm은 필수 입력값입니다.");
        }
    }

    public static void validateNewPassword(String newPassword) {
        if (StringUtils.isBlank(newPassword)) {
            throw new BadRequestException("new_password는 필수 입력값입니다.");
        }
    }

    public static void validateAccessToken(String accessToken) {
        if (StringUtils.isBlank(accessToken)) {
            throw new BadRequestException("access_token은 필수 입력값입니다.");
        }
    }

    public static void verifyDuplicateNickname(String nickname, Function<String, Boolean> existsByNickname) {
        validateNickname(nickname);
        if (existsByNickname.apply(nickname)) {
            throw new StateConflictException("이미 존재하는 nickname입니다. nickname: " + nickname);
        }
    }

    public static User getUserOrThrow(UUID id, Function<UUID, Optional<User>> fetchOneById) {
        validateId(id);

        return fetchOneById.apply(id)
                           .orElseThrow(() -> new NotFoundException("존재하지 않는 user입니다. id: " + id));
    }
}
