package com.backend.commondataaccess.persistence.user.enums;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

@Getter
@AllArgsConstructor
public enum SnsProvider {
    GOOGLE(1, "Google");

    private static final Map<Integer, SnsProvider> snsProviderMap =
            Stream.of(values()).collect(toMap(SnsProvider::getCode, value -> value));

    private final Integer code;
    private final String value;

    public static SnsProvider from(int snsProviderCode) {
        SnsProvider snsProvider = snsProviderMap.get(snsProviderCode);
        if (ObjectUtils.isEmpty(snsProvider)) {
            throw new IllegalArgumentException("잘못된 SnsProvider 타입입니다.");
        }

        return snsProvider;
    }
}
