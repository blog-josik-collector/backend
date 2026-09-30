package com.backend.interactionservice.postreport.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.exception.StateConflictException;
import com.backend.commondataaccess.persistence.common.enums.PostReportType;
import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.commondataaccess.persistence.post.Post;
import com.backend.commondataaccess.persistence.report.PostReport;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.interactionservice.post.repository.CollectSourcePostQueryRepository;
import com.backend.interactionservice.post.repository.PostQueryRepository;
import com.backend.interactionservice.post.service.PostService;
import com.backend.interactionservice.postreport.repository.PostReportQueryRepository;
import com.backend.interactionservice.postreport.repository.PostReportRepository;
import com.backend.interactionservice.postreport.service.dto.PostReportDto;
import com.backend.interactionservice.user.service.UserService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
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

@DisplayName("PostReportService 테스트")
@ExtendWith(MockitoExtension.class)
class PostReportServiceTest {

    @Spy
    @InjectMocks
    private PostReportService postReportService;

    @Mock
    private PostReportRepository postReportRepository;

    @Mock
    private PostReportQueryRepository queryRepository;

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private CollectSourcePostQueryRepository collectSourcePostQueryRepository;

    @Mock
    private PostService postService;

    @Mock
    private UserService userService;

    private User mockUser;
    private Post mockPost;
    private PostReport mockReport;

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

        mockReport = PostReport.builder()
                               .id(UUID.randomUUID())
                               .user(mockUser)
                               .post(mockPost)
                               .reportStatus(ReportStatus.PENDING)
                               .postReportType(PostReportType.BROKEN_LINK)
                               .content("링크가 깨져 있습니다.")
                               .build();
    }

    @DisplayName("게시글 신고 등록 테스트")
    @Nested
    class CreateReportTest {

        @Test
        void 게시글을_신고하면_PENDING_상태로_저장하고_total_report_count를_증가시킨다() {
            // given
            Mockito.doReturn(false).when(queryRepository).existsPendingByUserIdAndPostId(mockUser.id(), mockPost.id());
            Mockito.doReturn(mockUser).when(userService).getUser(mockUser.id());
            Mockito.doReturn(mockPost).when(postService).getPost(mockPost.id());
            Mockito.doReturn(mockReport).when(postReportRepository).saveAndFlush(any(PostReport.class));
            Mockito.doReturn(Map.of(mockPost.id(), "post_title")).when(collectSourcePostQueryRepository).findTitlesByIds(anyCollection());

            // when
            PostReportDto result = postReportService.createReport(mockUser.id(), mockPost.id(), PostReportType.BROKEN_LINK, "링크가 깨져 있습니다.");

            // then
            ArgumentCaptor<PostReport> captor = ArgumentCaptor.forClass(PostReport.class);
            Mockito.verify(postReportRepository).saveAndFlush(captor.capture());
            Assertions.assertThat(captor.getValue().reportStatus()).isEqualTo(ReportStatus.PENDING);
            Assertions.assertThat(captor.getValue().postReportType()).isEqualTo(PostReportType.BROKEN_LINK);
            Mockito.verify(postQueryRepository).incrementTotalReportCount(mockPost.id());

            Assertions.assertThat(result.id()).isEqualTo(mockReport.id());
            Assertions.assertThat(result.title()).isEqualTo("post_title");
            Assertions.assertThat(result.nickname()).isEqualTo(mockUser.nickname());
            Assertions.assertThat(result.status()).isEqualTo(ReportStatus.PENDING);
        }

        @Test
        void 처리_대기중인_신고가_있으면_신고에_실패한다() {
            // given
            Mockito.doReturn(true).when(queryRepository).existsPendingByUserIdAndPostId(mockUser.id(), mockPost.id());

            // when & then
            Assertions.assertThatThrownBy(() -> postReportService.createReport(mockUser.id(), mockPost.id(), PostReportType.OTHER, "content"))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("이미 처리 대기 중인 게시글 신고가 있습니다.");
            Mockito.verify(postReportRepository, Mockito.never()).saveAndFlush(any());
        }

        @Test
        void reportType이_null이면_신고에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postReportService.createReport(mockUser.id(), mockPost.id(), null, "content"))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("신고 유형(reportType)은 필수 입력값입니다.");
        }
    }

    @DisplayName("게시글 신고 목록 조회 테스트")
    @Nested
    class GetReportsTest {

        @Test
        void 날짜_필터를_KST_기준_범위로_변환하고_게시글_제목을_함께_반환한다() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            LocalDate startDate = LocalDate.of(2026, 5, 1);
            LocalDate endDate = LocalDate.of(2026, 5, 31);
            OffsetDateTime expectedStart = OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.ofHours(9));
            OffsetDateTime expectedEnd = OffsetDateTime.of(2026, 5, 31, 23, 59, 59, 0, ZoneOffset.ofHours(9));
            PostReport reportWithoutPost = PostReport.builder()
                                                     .id(UUID.randomUUID())
                                                     .reportStatus(ReportStatus.PENDING)
                                                     .postReportType(PostReportType.OTHER)
                                                     .content("content")
                                                     .build();
            OffsetPageResult<PostReport> page = new OffsetPageResult<>(2L, 0, 20, List.of(mockReport, reportWithoutPost));

            Mockito.doReturn(page).when(queryRepository).fetchPage(ReportStatus.PENDING,
                                                                   PostReportType.BROKEN_LINK,
                                                                   expectedStart,
                                                                   expectedEnd,
                                                                   pageable);
            Mockito.doReturn(Map.of(mockPost.id(), "post_title")).when(collectSourcePostQueryRepository).findTitlesByIds(anyCollection());

            // when
            OffsetPageResult<PostReportDto> result = postReportService.getReports(ReportStatus.PENDING,
                                                                                  PostReportType.BROKEN_LINK,
                                                                                  startDate,
                                                                                  endDate,
                                                                                  pageable);

            // then
            Assertions.assertThat(result.getTotalCount()).isEqualTo(2L);
            Assertions.assertThat(result.getItems().get(0).title()).isEqualTo("post_title");
            Assertions.assertThat(result.getItems().get(0).nickname()).isEqualTo(mockUser.nickname());
            Assertions.assertThat(result.getItems().get(1).title()).isNull();
            Assertions.assertThat(result.getItems().get(1).nickname()).isNull();
        }

        @Test
        void 결과가_비어있으면_제목_조회없이_빈_목록을_반환한다() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            OffsetPageResult<PostReport> page = new OffsetPageResult<>(0L, 0, 20, List.of());

            Mockito.doReturn(page).when(queryRepository).fetchPage(isNull(), isNull(), isNull(), isNull(), eq(pageable));

            // when
            OffsetPageResult<PostReportDto> result = postReportService.getReports(null, null, null, null, pageable);

            // then
            Assertions.assertThat(result.getItems()).isEmpty();
            Mockito.verify(collectSourcePostQueryRepository, Mockito.never()).findTitlesByIds(anyCollection());
        }
    }

    @DisplayName("게시글 신고 상태 변경 테스트")
    @Nested
    class ChangeStatusTest {

        @Test
        void RESOLVED_DELETED로_변경하면_게시글도_삭제_처리된다() {
            // given
            Mockito.doReturn(Optional.of(mockReport)).when(queryRepository).fetchOneById(mockReport.id());
            Mockito.doReturn(Map.of()).when(collectSourcePostQueryRepository).findTitlesByIds(anyCollection());

            // when
            PostReportDto result = postReportService.changeStatus(mockReport.id(), ReportStatus.RESOLVED_DELETED);

            // then
            Assertions.assertThat(result.status()).isEqualTo(ReportStatus.RESOLVED_DELETED);
            Assertions.assertThat(mockPost.postStatus()).isEqualTo(PostStatus.DELETED);
            Assertions.assertThat(mockPost.isDelete()).isTrue();
        }

        @Test
        void 이미_삭제된_게시글이면_게시글_상태는_그대로_유지된다() {
            // given
            Post deletedPost = Post.builder()
                                   .id(UUID.randomUUID())
                                   .postStatus(PostStatus.DELETED)
                                   .build();
            PostReport report = PostReport.builder()
                                          .id(UUID.randomUUID())
                                          .user(mockUser)
                                          .post(deletedPost)
                                          .reportStatus(ReportStatus.PENDING)
                                          .postReportType(PostReportType.OTHER)
                                          .content("content")
                                          .build();
            Mockito.doReturn(Optional.of(report)).when(queryRepository).fetchOneById(report.id());
            Mockito.doReturn(Map.of()).when(collectSourcePostQueryRepository).findTitlesByIds(anyCollection());

            // when
            PostReportDto result = postReportService.changeStatus(report.id(), ReportStatus.RESOLVED_DELETED);

            // then
            Assertions.assertThat(result.status()).isEqualTo(ReportStatus.RESOLVED_DELETED);
            Assertions.assertThat(deletedPost.postStatus()).isEqualTo(PostStatus.DELETED);
            Assertions.assertThat(deletedPost.isDelete()).isFalse();
        }

        @Test
        void REJECTED_KEEP으로_변경하면_게시글은_유지된다() {
            // given
            Mockito.doReturn(Optional.of(mockReport)).when(queryRepository).fetchOneById(mockReport.id());
            Mockito.doReturn(Map.of()).when(collectSourcePostQueryRepository).findTitlesByIds(anyCollection());

            // when
            PostReportDto result = postReportService.changeStatus(mockReport.id(), ReportStatus.REJECTED_KEEP);

            // then
            Assertions.assertThat(result.status()).isEqualTo(ReportStatus.REJECTED_KEEP);
            Assertions.assertThat(mockPost.postStatus()).isEqualTo(PostStatus.ACTIVE);
        }

        @Test
        void 이미_처리된_신고는_상태_변경에_실패한다() {
            // given
            PostReport resolved = PostReport.builder()
                                            .id(UUID.randomUUID())
                                            .post(mockPost)
                                            .reportStatus(ReportStatus.REJECTED_KEEP)
                                            .build();
            Mockito.doReturn(Optional.of(resolved)).when(queryRepository).fetchOneById(resolved.id());

            // when & then
            Assertions.assertThatThrownBy(() -> postReportService.changeStatus(resolved.id(), ReportStatus.RESOLVED_DELETED))
                      .isInstanceOf(StateConflictException.class);
        }

        @Test
        void PENDING으로_변경하면_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postReportService.changeStatus(mockReport.id(), ReportStatus.PENDING))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("신고를 PENDING 으로 되돌릴 수 없습니다.");
        }

        @Test
        void 존재하지_않는_신고면_실패한다() {
            // given
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneById(any());

            // when & then
            Assertions.assertThatThrownBy(() -> postReportService.changeStatus(UUID.randomUUID(), ReportStatus.REJECTED_KEEP))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("존재하지 않는 게시글 신고입니다.");
        }
    }
}
