package com.backend.userservice.userauthentication.service;

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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class UserAuthenticationService {

    private final UserAuthenticationRepository userAuthenticationRepository;
    private final UserAuthenticationQueryRepository queryRepository;

    public UserAuthentication create(User user, String userId, String password, String passwordConfirm) {
        UserAuthenticationValidator.validateUser(user);
        UserAuthenticationValidator.validateIdentifier(userId);
        UserAuthenticationValidator.validateCredential(password);

        UserAuthenticationValidator.validateIsSameCredentialAndCredentialConfirm(password, passwordConfirm);
        UserAuthenticationValidator.verifyDuplicateIdentifier(userId, userAuthenticationRepository::existsByIdentifier);

        UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                  .user(user)
                                                                  .loginProvider(LoginProvider.LOCAL)
                                                                  .identifier(userId)
                                                                  .credential(password)
                                                                  .build();

        return userAuthenticationRepository.save(userAuthentication);
    }

    public UserAuthentication create(User user, String subject) {
        UserAuthenticationValidator.validateUser(user);
        UserAuthenticationValidator.validateIdentifier(subject);

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

        return UserAuthenticationValidator.getUserAuthenticationOrThrow(id, queryRepository::findOneById);
    }

    @Transactional(readOnly = true)
    public UserAuthentication getUserAuthentication(String identifier) {
        UserAuthenticationValidator.validateIdentifier(identifier);

        return UserAuthenticationValidator.getUserAuthenticationOrThrow(identifier, queryRepository::findOneByIdentifier);
    }

    @Transactional(readOnly = true)
    public List<UserAuthentication> getUserAuthenticationsByUser(UUID userId) {
        UserAuthenticationValidator.validateUserId(userId);

        return queryRepository.findAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Optional<UserAuthentication> findUserAuthentication(String identifier) {
        UserAuthenticationValidator.validateIdentifier(identifier);
        return queryRepository.findOneByIdentifier(identifier);
    }

    public void updatePassword(UUID userId, String password, String newPassword) {
        UserAuthenticationValidator.validateUserId(userId);

        List<UserAuthentication> userAuthentications = queryRepository.findAllByUserId(userId);

        //FIXME
        UserAuthentication userAuthentication = userAuthentications.stream()
                                                                   .filter(ua -> ua.loginProvider().equals(LoginProvider.LOCAL))
                                                                   .findFirst()
                                                                   .orElseThrow(() -> new IllegalArgumentException(LoginProvider.LOCAL.getValue() + " 계정만 비밀번호를 변경할 수 있습니다."));

        if (!StringUtils.equals(userAuthentication.credential(), password)) {
            throw new IllegalArgumentException("사용자의 password와 입력한 password가 다릅니다. 입력한 password: " + password);
        }

        userAuthentication.updateCredential(newPassword);
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
}
