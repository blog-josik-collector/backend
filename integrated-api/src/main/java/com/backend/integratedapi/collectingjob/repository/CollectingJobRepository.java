package com.backend.integratedapi.collectingjob.repository;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectingJobRepository extends JpaRepository<CollectingJob, UUID> {

}
