package com.backend.integratedworker.collectsourcepost.repository;

import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectSourcePostRepository extends JpaRepository<CollectSourcePost, UUID> {

    @Query(value = """
            SELECT * FROM collect_source_posts
            WHERE indexing_status = 'PENDING'
              AND deleted_at IS NULL
            ORDER BY last_collected_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<CollectSourcePost> findAllPendingCollectSourcePosts(@Param("batchSize") int batchSize);
}
