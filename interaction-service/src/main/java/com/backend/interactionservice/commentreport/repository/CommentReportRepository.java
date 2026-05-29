package com.backend.interactionservice.commentreport.repository;

import com.backend.commondataaccess.persistence.report.CommentReport;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, UUID> {

}
