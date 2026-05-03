package com.backend.integratedworker.collectingjob.repository;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectingJobRepository extends JpaRepository<CollectingJob, UUID> {

    @Query(value = """
        SELECT * FROM collecting_jobs
        WHERE job_status = 'PENDING'
        ORDER BY created_at
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<CollectingJob> pickPending(@Param("batchSize") int batchSize);
}
