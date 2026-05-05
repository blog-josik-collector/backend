package com.backend.integratedapi.indexingjob.repository;

import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndexingJobRepository extends JpaRepository<IndexingJob, UUID> {

}
