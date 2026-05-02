package com.backend.integratedapi.source.repository;

import com.backend.commondataaccess.persistence.source.CollectSource;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectSourceRepository extends JpaRepository<CollectSource, UUID> {

}
