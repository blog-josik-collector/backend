package com.backend.userservice.user.service.validator;

import com.backend.userservice.user.service.dto.UserDto;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserValidator {

    public static UnaryOperator<UserDto> validateUserId() {
        return userDto -> {
            validateUserId(userDto.userId());
            return userDto;
        };
    }

    public static UnaryOperator<UserDto> validateNickname() {
        return userDto -> {
            validateNickname(userDto.userId());
            return userDto;
        };
    }

    public static void validateId(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("id는 필수 입력값입니다.");
        }


    }

    public static void validateUserId(String userId) {
        if (StringUtils.isBlank(userId)) {
            throw new IllegalArgumentException("user_id는 필수 입력값입니다.");
        }
    }

    public static void validateNickname(String nickname) {
        if (StringUtils.isBlank(nickname)) {
            throw new IllegalArgumentException("nickname는 필수 입력값입니다.");
        }
    }

    public static void validateDuplicateUserId(String userId, Function<String, Boolean> existsByUserId) {
        validateUserId(userId);
        if (existsByUserId.apply(userId)) {
            throw new IllegalArgumentException("이미 존재하는 user_id입니다. user_id: " + userId);
        }
    }

    public static void validateDuplicateNickname(String nickname, Function<String, Boolean> existsByNickname) {
        validateNickname(nickname);
        if (existsByNickname.apply(nickname)) {
            throw new IllegalArgumentException("이미 존재하는 nickname입니다. nickname: " + nickname);
        }
    }

}
