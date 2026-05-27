package com.backend.commondataaccess.persistence.post;

import com.backend.commondataaccess.persistence.common.BaseEntity;
import com.backend.commondataaccess.persistence.common.enums.PostCommentStatus;
import com.backend.commondataaccess.persistence.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name = "post_comments")
@Entity
public class PostComment extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private PostComment parentComment;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "total_report_count")
    private int totalReportCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_comment_status", nullable = false)
    private PostCommentStatus postCommentStatus;

    public void updateContent(String content) {
        this.content = content;
    }

    /**
     * 작성자 본인이 삭제. status를 DELETED 로 바꾸고 deleted_at 도 함께 채워서 soft-delete 한다.
     */
    public void softDelete() {
        this.postCommentStatus = PostCommentStatus.DELETED;
        this.delete();
    }

    public boolean isReplyComment() {
        return parentComment != null;
    }

    public boolean isOwnedBy(UUID userId) {
        return user != null && user.id().equals(userId);
    }
}
