package com.backend.commondataaccess.persistence.report;

import com.backend.commondataaccess.persistence.common.BaseEntity;
import com.backend.commondataaccess.persistence.common.enums.PostReportType;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.commondataaccess.persistence.post.Post;
import com.backend.commondataaccess.persistence.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "post_reports")
@Entity
public class PostReport extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;

    @Enumerated(EnumType.STRING)
    private ReportStatus reportStatus;

    @Enumerated(EnumType.STRING)
    private PostReportType postReportType;

    private String content;

    /**
     * 신고 처리 상태를 변경한다. 이미 처리(PENDING이 아님)된 신고는 더 이상 변경할 수 없다.
     */
    public void changeStatus(ReportStatus newStatus) {
        if (this.reportStatus != ReportStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 신고는 상태를 변경할 수 없습니다.");
        }
        if (newStatus == null || newStatus == ReportStatus.PENDING) {
            throw new IllegalArgumentException("PENDING 으로 되돌리는 변경은 허용되지 않습니다.");
        }
        this.reportStatus = newStatus;
    }
}
