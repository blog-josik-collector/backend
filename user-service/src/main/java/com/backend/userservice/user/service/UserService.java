package com.backend.userservice.user.service;

import com.backend.commondb.user.enums.LoginType;
import com.backend.commondb.user.User;
import com.backend.commondb.user.enums.UserType;
import com.backend.userservice.common.validator.ValidationFlow;
import com.backend.userservice.user.repository.UserQueryRepository;
import com.backend.userservice.user.repository.UserRepository;
import com.backend.userservice.user.service.dto.UserDto;
import com.backend.userservice.user.service.validator.UserValidator;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserQueryRepository userQueryRepository;

    public UserDto create(UserDto userDto) {
        ValidationFlow.start(userDto)
                      .next(UserValidator.validateUserId())
                      .next(UserValidator.validateNickname())
                      .end();

        UserValidator.validateDuplicateUserId(userDto.userId(), userRepository::existsByUserId);
        UserValidator.validateDuplicateNickname(userDto.nickname(), userRepository::existsByNickname);

        User user = User.builder()
                        .userType(UserType.USER)
                        .loginType(LoginType.DIRECT)
                        .userId(userDto.userId())
                        .password(userDto.password())
                        .nickname(userDto.nickname())
                        .build();

        return UserDto.from(userRepository.save(user));
    }

    public UserDto getUser(String id) {
        UserValidator.validateId(id);

        UUID uuid = UUID.fromString(id);
        User user = userQueryRepository.findById(uuid);
        return UserDto.from(Objects.requireNonNull(user));
    }
}
