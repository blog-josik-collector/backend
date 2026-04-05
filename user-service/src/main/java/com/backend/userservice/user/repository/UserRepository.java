package com.backend.userservice.user.repository;

import com.backend.commondataaccess.persistence.user.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUserId(String userId);

    boolean existsByNickname(String nickname);
}
