package com.backend.integratedworker.collectsource.repository;

import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectSourceRepository extends JpaRepository<CollectSource, UUID> {
}
