package com.backend.userservice.userauthentication.service;

import com.backend.commondataaccess.persistence.common.BaseEntity;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.UserAuthentication;
import com.backend.commondataaccess.persistence.user.enums.LoginProvider;
import com.backend.userservice.userauthentication.repository.UserAuthenticationQueryRepository;
import com.backend.userservice.userauthentication.repository.UserAuthenticationRepository;
import com.backend.userservice.userauthentication.service.validator.UserAuthenticationValidator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class UserAuthenticationService {

    private final UserAuthenticationRepository userAuthenticationRepository;
    private final UserAuthenticationQueryRepository queryRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAuthentication create(User user, String loginId, String password, String passwordConfirm) {
        UserAuthenticationValidator.validateUser(user);
        UserAuthenticationValidator.validateIdentifier(loginId);
        UserAuthenticationValidator.validateCredential(password);

        UserAuthenticationValidator.validateIsSameCredentialAndCredentialConfirm(password, passwordConfirm);
        UserAuthenticationValidator.verifyDuplicateIdentifier(loginId, userAuthenticationRepository::existsByIdentifier);

        String encodedPassword = passwordEncoder.encode(password);

        UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                  .user(user)
                                                                  .loginProvider(LoginProvider.LOCAL)
                                                                  .identifier(loginId)
                                                                  .credential(encodedPassword)
                                                                  .build();

        return userAuthenticationRepository.save(userAuthentication);
    }

    public UserAuthentication create(User user, String subject) {
        UserAuthenticationValidator.validateUser(user);
        UserAuthenticationValidator.validateIdentifier(subject);

        UserAuthenticationValidator.verifyDuplicateIdentifier(subject, userAuthenticationRepository::existsByIdentifier);

        UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                  .user(user)
                                                                  .loginProvider(LoginProvider.GOOGLE)
                                                                  .identifier(subject)
                                                                  .build();

        return userAuthenticationRepository.save(userAuthentication);
    }

    @Transactional(readOnly = true)
    public UserAuthentication getUserAuthentication(UUID id) {
        UserAuthenticationValidator.validateId(id);

        return UserAuthenticationValidator.getUserAuthenticationOrThrow(id, queryRepository::fetchOneById);
    }

    @Transactional(readOnly = true)
    public UserAuthentication getUserAuthentication(String identifier) {
        UserAuthenticationValidator.validateIdentifier(identifier);

        return UserAuthenticationValidator.getUserAuthenticationOrThrow(identifier, queryRepository::fetchOneByIdentifier);
    }

    @Transactional(readOnly = true)
    public List<UserAuthentication> getUserAuthenticationsByUser(UUID userId) {
        UserAuthenticationValidator.validateUserId(userId);

        return queryRepository.findAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Optional<UserAuthentication> findUserAuthentication(String identifier) {
        UserAuthenticationValidator.validateIdentifier(identifier);
        return queryRepository.fetchOneByIdentifier(identifier);
    }

    public void updatePassword(UUID userId, String password, String newPassword) {
        UserAuthenticationValidator.validateUserId(userId);

        List<UserAuthentication> userAuthentications = queryRepository.findAllByUserId(userId);

        UserAuthentication userAuthentication = userAuthentications.stream()
                                                                   .filter(ua -> ua.loginProvider().equals(LoginProvider.LOCAL))
                                                                   .findFirst()
                                                                   .orElseThrow(() -> new IllegalArgumentException(LoginProvider.LOCAL.getValue() + " 계정만 비밀번호를 변경할 수 있습니다."));

        // 순서 주의: (평문 비밀번호, DB에 저장된 암호화된 비밀번호)
        if (!passwordEncoder.matches(password, userAuthentication.credential())) {
            throw new IllegalArgumentException("사용자의 password와 입력한 password가 다릅니다. 입력한 password: " + password);
        }

        String encodedPassword = passwordEncoder.encode(newPassword);

        userAuthentication.updateCredential(encodedPassword);
    }

    public void merge(UUID targetId, User sourceUser) {
        UserAuthenticationValidator.validateId(targetId);
        UserAuthenticationValidator.validateUser(sourceUser);

        UserAuthentication targetUserAuthentication = getUserAuthentication(targetId);
        User targetUser = targetUserAuthentication.user();

        if (targetUser.id().equals(sourceUser.id())) {
            throw new IllegalArgumentException("동일한 사용자끼리는 정보를 합칠 수 없습니다. user_id: " + sourceUser.id());
        }

        List<UserAuthentication> sourceUserAuthentications = getUserAuthenticationsByUser(sourceUser.id());

        sourceUserAuthentications.forEach(userAuthentication -> userAuthentication.user().delete());

        sourceUserAuthentications.forEach(userAuthentication -> userAuthentication.updateUser(targetUser));
    }

    public void deleteAll(UUID userId) {
        UserAuthenticationValidator.validateUserId(userId);

        getUserAuthenticationsByUser(userId).forEach(BaseEntity::delete);
    }
}
