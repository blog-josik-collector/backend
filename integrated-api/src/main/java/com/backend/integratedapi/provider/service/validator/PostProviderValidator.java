package com.backend.integratedapi.provider.service.validator;

import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedapi.provider.service.dto.PostProviderDto;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PostProviderValidator {

    public static UnaryOperator<PostProviderDto> validateId() {
        return postProviderDto -> {
            validateId(postProviderDto.id());
            return postProviderDto;
        };
    }

    public static UnaryOperator<PostProviderDto> validateName() {
        return postProviderDto -> {
            validateName(postProviderDto.name());
            return postProviderDto;
        };
    }

    public static UnaryOperator<PostProviderDto> validateBaseUrl() {
        return postProviderDto -> {
            validateBaseUrl(postProviderDto.baseUrl());
            return postProviderDto;
        };
    }

    public static UnaryOperator<PostProviderDto> validateDescription() {
        return postProviderDto -> {
            validateDescription(postProviderDto.description());
            return postProviderDto;
        };
    }

    public static void validateName(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name은 필수 입력값입니다.");
        }
    }

    public static void validateBaseUrl(String baseUrl) {
        if (StringUtils.isBlank(baseUrl)) {
            throw new IllegalArgumentException("baseUrl은 필수 입력값입니다.");
        }
    }

    public static void validateDescription(String description) {
        if (StringUtils.isBlank(description)) {
            throw new IllegalArgumentException("description은 필수 입력값입니다.");
        }
    }

    public static void validateId(UUID id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("id는 필수 입력값입니다.");
        }
    }

    public static void verifyDuplicateName(String name, Function<String, Boolean> existsByName) {
        validateName(name);
        if (existsByName.apply(name)) {
            throw new IllegalArgumentException("이미 존재하는 postProvider입니다. name: " + name);
        }
    }

    public static PostProvider getPostProviderOrThrow(UUID id, Function<UUID, Optional<PostProvider>> findById) {
        validateId(id);

        return findById.apply(id)
                       .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 postProvider입니다. id: " + id));
    }
}
