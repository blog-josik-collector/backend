package com.backend.commondataaccess.persistence.user.enums;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

@Getter
@AllArgsConstructor
public enum LoginProvider {
    LOCAL(1, "LoginId/Password 입력 방식"),
    GOOGLE(2, "Google OAuth 방식");

    private static final Map<Integer, LoginProvider> loginProviderMap =
            Stream.of(values()).collect(toMap(LoginProvider::getCode, value -> value));

    private final Integer code;
    private final String value;

    public static LoginProvider from(int loginProviderCode) {
        LoginProvider loginProvider = loginProviderMap.get(loginProviderCode);
        if (ObjectUtils.isEmpty(loginProvider)) {
            throw new IllegalArgumentException("잘못된 LoginProvider 타입입니다.");
        }

        return loginProvider;
    }
}
