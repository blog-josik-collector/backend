package com.backend.interactionservice.commentreport.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.exception.StateConflictException;
import com.backend.commondataaccess.persistence.common.enums.CommentReportType;
import com.backend.commondataaccess.persistence.common.enums.PostCommentStatus;
import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.commondataaccess.persistence.post.Post;
import com.backend.commondataaccess.persistence.post.PostComment;
import com.backend.commondataaccess.persistence.report.CommentReport;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.interactionservice.commentreport.repository.CommentReportQueryRepository;
import com.backend.interactionservice.commentreport.repository.CommentReportRepository;
import com.backend.interactionservice.commentreport.service.dto.CommentReportDto;
import com.backend.interactionservice.post.repository.PostQueryRepository;
import com.backend.interactionservice.postcomment.repository.PostCommentQueryRepository;
import com.backend.interactionservice.postcomment.service.PostCommentService;
import com.backend.interactionservice.user.service.UserService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DisplayName("CommentReportService 테스트")
@ExtendWith(MockitoExtension.class)
class CommentReportServiceTest {

    @Spy
    @InjectMocks
    private CommentReportService commentReportService;

    @Mock
    private CommentReportRepository commentReportRepository;

    @Mock
    private CommentReportQueryRepository queryRepository;

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private PostCommentQueryRepository postCommentQueryRepository;

    @Mock
    private PostCommentService postCommentService;

    @Mock
    private UserService userService;

    private User mockUser;
    private Post mockPost;
    private PostComment mockComment;
    private CommentReport mockReport;

    @BeforeEach
    void init() {
        mockUser = User.builder()
                       .id(UUID.randomUUID())
                       .userType(UserType.USER)
                       .nickname("test_nickname")
                       .build();

        mockPost = Post.builder()
                       .id(UUID.randomUUID())
                       .postStatus(PostStatus.ACTIVE)
                       .build();

        mockComment = PostComment.builder()
                                 .id(UUID.randomUUID())
                                 .user(mockUser)
                                 .post(mockPost)
                                 .content("test_comment")
                                 .postCommentStatus(PostCommentStatus.ACTIVE)
                                 .build();

        mockReport = createReport(mockComment, ReportStatus.PENDING);
    }

    private CommentReport createReport(PostComment comment, ReportStatus status) {
        return CommentReport.builder()
                            .id(UUID.randomUUID())
                            .user(mockUser)
                            .comment(comment)
                            .reportStatus(status)
                            .commentReportType(CommentReportType.ADULT)
                            .content("광고성 댓글입니다.")
                            .build();
    }

    @DisplayName("댓글 신고 등록 테스트")
    @Nested
    class CreateReportTest {

        @Test
        void 댓글을_신고하면_PENDING_상태로_저장하고_total_report_count를_증가시킨다() {
            // given
            Mockito.doReturn(false).when(queryRepository).existsPendingByUserIdAndCommentId(mockUser.id(), mockComment.id());
            Mockito.doReturn(mockUser).when(userService).getUser(mockUser.id());
            Mockito.doReturn(mockComment).when(postCommentService).getComment(mockComment.id());
            Mockito.doReturn(mockReport).when(commentReportRepository).saveAndFlush(any(CommentReport.class));

            // when
            CommentReportDto result = commentReportService.createReport(mockUser.id(), mockComment.id(), CommentReportType.ADULT, "광고성 댓글입니다.");

            // then
            ArgumentCaptor<CommentReport> captor = ArgumentCaptor.forClass(CommentReport.class);
            Mockito.verify(commentReportRepository).saveAndFlush(captor.capture());
            Assertions.assertThat(captor.getValue().reportStatus()).isEqualTo(ReportStatus.PENDING);
            Assertions.assertThat(captor.getValue().comment()).isSameAs(mockComment);
            Mockito.verify(postCommentQueryRepository).incrementTotalReportCount(mockComment.id());

            Assertions.assertThat(result.id()).isEqualTo(mockReport.id());
            Assertions.assertThat(result.commentContent()).isEqualTo(mockComment.content());
            Assertions.assertThat(result.nickname()).isEqualTo(mockUser.nickname());
            Assertions.assertThat(result.reportType()).isEqualTo(CommentReportType.ADULT);
        }

        @Test
        void 처리_대기중인_신고가_있으면_신고에_실패한다() {
            // given
            Mockito.doReturn(true).when(queryRepository).existsPendingByUserIdAndCommentId(mockUser.id(), mockComment.id());

            // when & then
            Assertions.assertThatThrownBy(() -> commentReportService.createReport(mockUser.id(), mockComment.id(), CommentReportType.OTHER, "content"))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("이미 처리 대기 중인 댓글 신고가 있습니다.");
            Mockito.verify(commentReportRepository, Mockito.never()).saveAndFlush(any());
        }

        @Test
        void commentId가_null이면_신고에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> commentReportService.createReport(mockUser.id(), null, CommentReportType.OTHER, "content"))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("commentId는 필수 입력값입니다.");
        }
    }

    @DisplayName("댓글 신고 목록 조회 테스트")
    @Nested
    class GetReportsTest {

        @Test
        void 날짜_필터를_KST_기준_범위로_변환하여_조회한다() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            OffsetDateTime expectedStart = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.ofHours(9));
            OffsetDateTime expectedEnd = OffsetDateTime.of(2026, 5, 31, 23, 59, 59, 0, ZoneOffset.ofHours(9));
            OffsetPageResult<CommentReport> page = new OffsetPageResult<>(1L, 0, 20, List.of(mockReport));

            Mockito.doReturn(page).when(queryRepository).fetchPage(ReportStatus.PENDING,
                                                                   CommentReportType.ADULT,
                                                                   expectedStart,
                                                                   expectedEnd,
                                                                   pageable);

            // when
            OffsetPageResult<CommentReportDto> result = commentReportService.getReports(ReportStatus.PENDING,
                                                                                        CommentReportType.ADULT,
                                                                                        LocalDate.of(2026, 5, 1),
                                                                                        LocalDate.of(2026, 5, 31),
                                                                                        pageable);

            // then
            Assertions.assertThat(result.getTotalCount()).isEqualTo(1L);
            Assertions.assertThat(result.getItems().get(0).id()).isEqualTo(mockReport.id());
            Assertions.assertThat(result.getItems().get(0).commentContent()).isEqualTo(mockComment.content());
        }

        @Test
        void 필터가_없으면_null_조건으로_조회한다() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            CommentReport reportWithoutRelations = CommentReport.builder()
                                                                .id(UUID.randomUUID())
                                                                .reportStatus(ReportStatus.PENDING)
                                                                .build();
            OffsetPageResult<CommentReport> page = new OffsetPageResult<>(1L, 0, 20, List.of(reportWithoutRelations));

            Mockito.doReturn(page).when(queryRepository).fetchPage(isNull(), isNull(), isNull(), isNull(), eq(pageable));

            // when
            OffsetPageResult<CommentReportDto> result = commentReportService.getReports(null, null, null, null, pageable);

            // then
            Assertions.assertThat(result.getItems()).hasSize(1);
            Assertions.assertThat(result.getItems().get(0).commentContent()).isNull();
            Assertions.assertThat(result.getItems().get(0).nickname()).isNull();
        }
    }

    @DisplayName("댓글 신고 상태 변경 테스트")
    @Nested
    class ChangeStatusTest {

        @Test
        void RESOLVED_DELETED로_변경하면_활성_댓글을_삭제하고_comment_count를_감소시킨다() {
            // given
            Mockito.doReturn(Optional.of(mockReport)).when(queryRepository).fetchOneById(mockReport.id());

            // when
            CommentReportDto result = commentReportService.changeStatus(mockReport.id(), ReportStatus.RESOLVED_DELETED);

            // then
            Assertions.assertThat(result.status()).isEqualTo(ReportStatus.RESOLVED_DELETED);
            Assertions.assertThat(mockComment.postCommentStatus()).isEqualTo(PostCommentStatus.DELETED);
            Mockito.verify(postQueryRepository).decrementCommentCount(mockPost.id());
        }

        @Test
        void BLOCKED_댓글을_RESOLVED_DELETED로_변경하면_comment_count는_감소시키지_않는다() {
            // given
            PostComment blocked = PostComment.builder()
                                             .id(UUID.randomUUID())
                                             .user(mockUser)
                                             .post(mockPost)
                                             .content("blocked")
                                             .postCommentStatus(PostCommentStatus.BLOCKED)
                                             .build();
            CommentReport report = createReport(blocked, ReportStatus.PENDING);
            Mockito.doReturn(Optional.of(report)).when(queryRepository).fetchOneById(report.id());

            // when
            commentReportService.changeStatus(report.id(), ReportStatus.RESOLVED_DELETED);

            // then
            Assertions.assertThat(blocked.postCommentStatus()).isEqualTo(PostCommentStatus.DELETED);
            Mockito.verify(postQueryRepository, Mockito.never()).decrementCommentCount(any());
        }

        @Test
        void 이미_삭제된_댓글이면_댓글_상태는_그대로_유지된다() {
            // given
            PostComment deleted = PostComment.builder()
                                             .id(UUID.randomUUID())
                                             .user(mockUser)
                                             .post(mockPost)
                                             .content("deleted")
                                             .postCommentStatus(PostCommentStatus.DELETED)
                                             .build();
            CommentReport report = createReport(deleted, ReportStatus.PENDING);
            Mockito.doReturn(Optional.of(report)).when(queryRepository).fetchOneById(report.id());

            // when
            commentReportService.changeStatus(report.id(), ReportStatus.RESOLVED_DELETED);

            // then
            Assertions.assertThat(deleted.isDelete()).isFalse();
            Mockito.verify(postQueryRepository, Mockito.never()).decrementCommentCount(any());
        }

        @Test
        void REJECTED_KEEP으로_변경하면_댓글은_유지된다() {
            // given
            Mockito.doReturn(Optional.of(mockReport)).when(queryRepository).fetchOneById(mockReport.id());

            // when
            CommentReportDto result = commentReportService.changeStatus(mockReport.id(), ReportStatus.REJECTED_KEEP);

            // then
            Assertions.assertThat(result.status()).isEqualTo(ReportStatus.REJECTED_KEEP);
            Assertions.assertThat(mockComment.postCommentStatus()).isEqualTo(PostCommentStatus.ACTIVE);
            Mockito.verify(postQueryRepository, Mockito.never()).decrementCommentCount(any());
        }

        @Test
        void 이미_처리된_신고는_상태_변경에_실패한다() {
            // given
            CommentReport resolved = createReport(mockComment, ReportStatus.REJECTED_KEEP);
            Mockito.doReturn(Optional.of(resolved)).when(queryRepository).fetchOneById(resolved.id());

            // when & then
            Assertions.assertThatThrownBy(() -> commentReportService.changeStatus(resolved.id(), ReportStatus.RESOLVED_DELETED))
                      .isInstanceOf(StateConflictException.class);
        }

        @Test
        void newStatus가_null이면_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> commentReportService.changeStatus(mockReport.id(), null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("변경할 신고 상태는 필수 입력값입니다.");
        }

        @Test
        void 존재하지_않는_신고면_실패한다() {
            // given
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneById(any());

            // when & then
            Assertions.assertThatThrownBy(() -> commentReportService.changeStatus(UUID.randomUUID(), ReportStatus.REJECTED_KEEP))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("존재하지 않는 댓글 신고입니다.");
        }
    }
}
