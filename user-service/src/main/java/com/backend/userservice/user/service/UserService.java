package com.backend.userservice.user.service;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.userservice.common.validator.ValidationFlow;
import com.backend.userservice.user.repository.UserQueryRepository;
import com.backend.userservice.user.repository.UserRepository;
import com.backend.userservice.user.service.dto.UserDto;
import com.backend.userservice.user.service.validator.UserValidator;
import com.backend.userservice.userauthentication.service.UserAuthenticationService;
import com.backend.userservice.utils.NicknameGenerateUtil;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserQueryRepository userQueryRepository;
    private final UserAuthenticationService userAuthenticationService;

    public UserDto create(UserDto userDto) {
        ValidationFlow.start(userDto)
                      .next(UserValidator.validateUserId())
                      .next(UserValidator.validateNickname())
                      .next(UserValidator.validatePasswordAndPasswordConfirm())
                      .end();

        UserValidator.verifyDuplicateNickname(userDto.nickname(), userRepository::existsByNickname);

        User user = User.builder()
                        .userType(UserType.USER)
                        .nickname(userDto.nickname())
                        .build();

        User savedUser = userRepository.save(user);

        userAuthenticationService.create(savedUser,
                                         userDto.loginId(),
                                         userDto.password(),
                                         userDto.passwordConfirm());

        return UserDto.from(savedUser);
    }

    public User create(String subject) {
        UserValidator.validateSubjectId(subject);

        String nickname = getSafeNickname();

        User user = User.builder()
                        .userType(UserType.USER)
                        .nickname(nickname)
                        .build();

        User savedUser = userRepository.save(user);

        userAuthenticationService.create(savedUser, subject);

        return savedUser;
    }

    @Transactional(readOnly = true)
    public UserDto getUserDto(UserDto userDto) {
        return UserDto.from(getUser(userDto.userId()));
    }

    @Transactional(readOnly = true)
    public UserDto getUserDto(UUID id) {
        User user = getUser(id);
        return UserDto.from(user);
    }

    @Transactional(readOnly = true)
    public User getUser(UUID id) {
        UserValidator.validateId(id);
        return UserValidator.getUserOrThrow(id, userQueryRepository::findById);
    }

    @Transactional(readOnly = true)
    public String getSafeNickname() {
        String nickname;
        int maxRetry = 5;
        int count = 0;

        do {
            nickname = NicknameGenerateUtil.generate();
            count++;
            if (count > maxRetry) {
                // 5번 이상 충돌 시 더 긴 랜덤값을 붙이거나 예외 처리
                nickname += System.currentTimeMillis() % 1000;
            }
        } while (userRepository.existsByNickname(nickname));

        return nickname;
    }

    public void update(UserDto userDto) {
        ValidationFlow.start(userDto)
                      .next(UserValidator.validateId())
                      .next(UserValidator.validateNickname())
                      .end();

        UserValidator.verifyDuplicateNickname(userDto.nickname(), userRepository::existsByNickname);

        User user = getUser(userDto.userId());

        user.update(userDto.nickname());
    }

    public void updatePassword(UUID id, String password, String newPassword) {
        UserValidator.validateId(id);
        UserValidator.validatePassword(password);
        UserValidator.validateNewPassword(newPassword);
        userAuthenticationService.updatePassword(id, password, newPassword);
    }

    public void merge(UUID authenticationId, UUID userId) {
        UserValidator.validateId(userId);
        User newUser = getUser(userId);

        userAuthenticationService.merge(authenticationId, newUser);
    }

    public void delete(UUID id) {
        UserValidator.validateId(id);
        User user = getUser(id);
        user.delete();
    }
}
