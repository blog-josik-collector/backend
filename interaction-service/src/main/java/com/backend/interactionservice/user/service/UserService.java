package com.backend.interactionservice.user.service;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.interactionservice.user.repository.UserQueryRepository;
import com.backend.interactionservice.user.service.validator.UserValidator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserQueryRepository userQueryRepository;

    @Transactional(readOnly = true)
    public User getUser(UUID id) {
        UserValidator.validateId(id);
        return UserValidator.getUserOrThrow(id, userQueryRepository::fetchOneById);
    }
}
