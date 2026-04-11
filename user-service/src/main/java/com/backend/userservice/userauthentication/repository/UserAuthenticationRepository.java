package com.backend.userservice.userauthentication.repository;

import com.backend.commondataaccess.persistence.user.UserAuthentication;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAuthenticationRepository extends JpaRepository<UserAuthentication, UUID> {

    boolean existsByIdentifier(String identifier);
}
