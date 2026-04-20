package com.backend.integratedapi.provider.repository;

import com.backend.commondataaccess.persistence.provider.PostProvider;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostProviderRepository extends JpaRepository<PostProvider, UUID> {

    boolean existsByName(String name);
}
