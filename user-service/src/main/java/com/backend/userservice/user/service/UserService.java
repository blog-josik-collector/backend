package com.backend.userservice.user.service;

import com.backend.commondb.user.User;
import com.backend.commondb.user.enums.LoginType;
import com.backend.commondb.user.enums.UserType;
import com.backend.userservice.common.validator.ValidationFlow;
import com.backend.userservice.user.repository.UserQueryRepository;
import com.backend.userservice.user.repository.UserRepository;
import com.backend.userservice.user.service.dto.UserDto;
import com.backend.userservice.user.service.validator.UserValidator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserQueryRepository userQueryRepository;

    public UserDto create(UserDto userDto) {
        ValidationFlow.start(userDto)
                      .next(UserValidator.validateUserId())
                      .next(UserValidator.validateNickname())
                      .next(UserValidator.validatePasswordAndPasswordConfirm())
                      .end();

        UserValidator.validateIsSamePasswordAndPasswordConfirm(userDto.password(), userDto.passwordConfirm());
        UserValidator.verifyDuplicateUserId(userDto.userId(), userRepository::existsByUserId);
        UserValidator.verifyDuplicateNickname(userDto.nickname(), userRepository::existsByNickname);

        User user = User.builder()
                        .userType(UserType.USER)
                        .loginType(LoginType.DIRECT)
                        .userId(userDto.userId())
                        .password(userDto.password())
                        .nickname(userDto.nickname())
                        .build();

        return UserDto.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserDto getUserDto(UserDto userDto) {
        return UserDto.from(getUser(userDto.id()));
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

    public void update(UserDto userDto) {
        ValidationFlow.start(userDto)
                      .next(UserValidator.validateId())
                      .next(UserValidator.validateNickname())
                      .end();

        UserValidator.verifyDuplicateNickname(userDto.nickname(), userRepository::existsByNickname);

        User user = getUser(userDto.id());

        user.update(userDto.nickname());
    }

    public void updatePassword(UUID id, String password, String newPassword) {
        UserValidator.validateId(id);
        UserValidator.validatePassword(password);
        UserValidator.validateNewPassword(newPassword);

        User user = getUser(id);

        if (!StringUtils.equals(user.password(), password)) {
            throw new IllegalArgumentException("사용자의 password와 입력한 password가 다릅니다. 입력한 password: + " + password);
        }

        user.updatePassword(newPassword);
    }

    public void delete(UUID id) {
        UserValidator.validateId(id);
        User user = getUser(id);
        user.delete();
    }
}
