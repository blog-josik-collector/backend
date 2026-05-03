package com.backend.integratedworker.collectsourcepost.repository;

import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectSourcePostRepository extends JpaRepository<CollectSourcePost, UUID> {

}
