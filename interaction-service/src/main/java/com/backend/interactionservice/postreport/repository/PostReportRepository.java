package com.backend.interactionservice.postreport.repository;

import com.backend.commondataaccess.persistence.report.PostReport;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostReportRepository extends JpaRepository<PostReport, UUID> {

}
