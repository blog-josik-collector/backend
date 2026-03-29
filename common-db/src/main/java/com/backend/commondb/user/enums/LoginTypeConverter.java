package com.backend.commondb.user.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LoginTypeConverter implements AttributeConverter<LoginType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(LoginType loginType) {
        return loginType.getCode();
    }

    @Override
    public LoginType convertToEntityAttribute(Integer loginTypeCode) {
        return LoginType.from(loginTypeCode);
    }
}
