package com.backend.userservice.user.service;

import com.backend.commondb.user.enums.LoginType;
import com.backend.commondb.user.User;
import com.backend.commondb.user.enums.UserType;
import com.backend.userservice.user.repository.UserRepository;
import com.backend.userservice.user.service.dto.UserDto;
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

    public UserDto create(UserDto userDto) {

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
        UUID uuid = UUID.fromString(id);
        User user = userRepository.findById(uuid).orElse(null);
        return UserDto.from(Objects.requireNonNull(user));
    }
}
