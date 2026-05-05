package com.backend.integratedworker.indexingjob.repository;

import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IndexingJobRepository extends JpaRepository<IndexingJob, UUID> {

    @Query(value = """
            SELECT * FROM indexing_jobs
            WHERE job_status = 'PENDING'
            ORDER BY created_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<IndexingJob> findAllPendingIndexingJobs(@Param("batchSize") int batchSize);
}
