package com.backend.userservice.user.repository;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByNicknameAndDeletedAtIsNull(String nickname);

    boolean existsByNicknameAndIdNotAndDeletedAtIsNull(String nickname, UUID id);

    boolean existsByUserTypeAndDeletedAtIsNull(UserType userType);
}
