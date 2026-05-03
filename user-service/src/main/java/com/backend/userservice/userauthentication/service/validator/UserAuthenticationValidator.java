package com.backend.userservice.userauthentication.service.validator;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.UserAuthentication;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserAuthenticationValidator {

    public static void validateUser(User user) {
        if (ObjectUtils.isEmpty(user)) {
            throw new IllegalArgumentException("user는 필수 입력값입니다.");
        }
    }

    public static void validateId(UUID id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("id는 필수 입력값입니다.");
        }
    }

    public static void validateIdentifier(String identifier) {
        if (StringUtils.isBlank(identifier)) {
            throw new IllegalArgumentException("identifier는 필수 입력값입니다.");
        }
    }

    public static void validateCredential(String credential) {
        if (StringUtils.isBlank(credential)) {
            throw new IllegalArgumentException("credential은 필수 입력값입니다.");
        }
    }

    public static void validateUserId(UUID userId) {
        if (ObjectUtils.isEmpty(userId)) {
            throw new IllegalArgumentException("user_id는 필수 입력값입니다.");
        }
    }

    public static void validateIsSameCredentialAndCredentialConfirm(String credential,
                                                                    String credentialConfirm) {
        if (StringUtils.isBlank(credential)) {
            throw new IllegalArgumentException("credential는 필수 입력값입니다.");
        }
        if (StringUtils.isBlank(credentialConfirm)) {
            throw new IllegalArgumentException("credential_confirm은 필수 입력값입니다.");
        }
        if (!StringUtils.equals(credential, credentialConfirm)) {
            throw new IllegalArgumentException("credential와 credential_confirm은 값은 동일해야합니다.");
        }
    }

    public static void verifyDuplicateIdentifier(String identifier, Function<String, Boolean> existsByIdentifier) {
        validateIdentifier(identifier);
        if (existsByIdentifier.apply(identifier)) {
            throw new IllegalArgumentException("이미 존재하는 identifier입니다. identifier: " + identifier);
        }
    }

    public static UserAuthentication getUserAuthenticationOrThrow(UUID id, Function<UUID, Optional<UserAuthentication>> fetchOneById) {
        validateId(id);

        return fetchOneById.apply(id)
                          .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 id입니다. id: " + id));
    }

    public static UserAuthentication getUserAuthenticationOrThrow(String identifier, Function<String, Optional<UserAuthentication>> fetchOneByIdentifier) {
        validateIdentifier(identifier);

        return fetchOneByIdentifier.apply(identifier)
                                  .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 identifier입니다. identifier: " + identifier));
    }
}
