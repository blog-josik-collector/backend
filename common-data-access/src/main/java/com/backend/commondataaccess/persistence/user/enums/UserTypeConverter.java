package com.backend.commondataaccess.persistence.user.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserTypeConverter implements AttributeConverter<UserType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(UserType userType) {
        if (userType == null) {
            return null;
        }
        return userType.getCode();
    }

    @Override
    public UserType convertToEntityAttribute(Integer userTypeCode) {
        if (userTypeCode == null) {
            return null;
        }
        return UserType.from(userTypeCode);
    }
}
