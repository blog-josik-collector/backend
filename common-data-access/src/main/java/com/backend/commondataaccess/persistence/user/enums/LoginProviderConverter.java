package com.backend.commondataaccess.persistence.user.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LoginProviderConverter implements AttributeConverter<LoginProvider, Integer> {

    @Override
    public Integer convertToDatabaseColumn(LoginProvider loginProvider) {
        // null이면 DB에도 null로 들어가도록 방어 로직 추가
        if (loginProvider == null) {
            return null;
        }
        return loginProvider.getCode();
    }

    @Override
    public LoginProvider convertToEntityAttribute(Integer loginProviderCode) {
        // DB 값이 null이거나 빈 문자열일 때의 처리
        if (loginProviderCode == null) {
            return null;
        }

        return LoginProvider.from(loginProviderCode);
    }
}
