package com.backend.commondataaccess.persistence.user.enums;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

@Getter
@AllArgsConstructor
public enum LoginType {
    DIRECT(1, "직접 가입"),
    SNS(2, "SNS 가입");

    private static final Map<Integer, LoginType> LoginTypeMap =
            Stream.of(values()).collect(toMap(LoginType::getCode, value -> value));

    private final Integer code;
    private final String value;

    public static LoginType from(int loginTypeCode) {
        LoginType loginType = LoginTypeMap.get(loginTypeCode);
        if (ObjectUtils.isEmpty(loginType)) {
            throw new IllegalArgumentException("잘못된 LoginType 타입입니다.");
        }

        return loginType;
    }
}
