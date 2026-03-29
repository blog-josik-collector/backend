package com.backend.commondb.user.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserTypeConverter implements AttributeConverter<UserType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(UserType userType) {
        return userType.getCode();
    }

    @Override
    public UserType convertToEntityAttribute(Integer userTypeCode) {
        return UserType.from(userTypeCode);
    }
}
