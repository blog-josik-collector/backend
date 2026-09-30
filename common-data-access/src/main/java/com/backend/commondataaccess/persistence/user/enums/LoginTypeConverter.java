package com.backend.commondataaccess.persistence.user.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LoginTypeConverter implements AttributeConverter<LoginType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(LoginType loginType) {
        if (loginType == null) {
            return null;
        }
        return loginType.getCode();
    }

    @Override
    public LoginType convertToEntityAttribute(Integer loginTypeCode) {
        if (loginTypeCode == null) {
            return null;
        }
        return LoginType.from(loginTypeCode);
    }
}
