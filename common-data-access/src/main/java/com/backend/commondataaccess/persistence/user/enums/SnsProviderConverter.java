package com.backend.commondataaccess.persistence.user.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SnsProviderConverter implements AttributeConverter<SnsProvider, Integer> {

    @Override
    public Integer convertToDatabaseColumn(SnsProvider snsProvider) {
        // null이면 DB에도 null로 들어가도록 방어 로직 추가
        if (snsProvider == null) {
            return null;
        }
        return snsProvider.getCode();
    }

    @Override
    public SnsProvider convertToEntityAttribute(Integer snsProviderCode) {
        // DB 값이 null이거나 빈 문자열일 때의 처리
        if (snsProviderCode == null) {
            return null;
        }

        return SnsProvider.from(snsProviderCode);
    }
}
