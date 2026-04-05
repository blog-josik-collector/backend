package com.backend.commondataaccess.persistence.user.enums;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

@Getter
@AllArgsConstructor
public enum UserType {
    USER(1, "일반 회원"),
    ADMIN(2, "운영진");

    private static final Map<Integer, UserType> UserTypeMap =
            Stream.of(values()).collect(toMap(UserType::getCode, value -> value));

    private final Integer code;
    private final String value;

    public static UserType from(int JoinStateCode) {
        UserType joinState = UserTypeMap.get(JoinStateCode);
        if (ObjectUtils.isEmpty(joinState)) {
            throw new IllegalArgumentException("잘못된 UserType 타입입니다.");
        }

        return joinState;
    }
}
