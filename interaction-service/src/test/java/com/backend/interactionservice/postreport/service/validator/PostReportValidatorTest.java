package com.backend.interactionservice.postreport.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.common.enums.PostReportType;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.commondataaccess.persistence.report.PostReport;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("PostReportValidator 테스트")
class PostReportValidatorTest {

    @Nested
    @DisplayName("필수값 검증")
    class RequiredFields {

        @Test
        void userId가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostReportValidator.validateUserId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("userId는 필수 입력값입니다.");
        }

        @Test
        void postId가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostReportValidator.validatePostId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("postId는 필수 입력값입니다.");
        }

        @Test
        void reportId가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostReportValidator.validateReportId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("reportId는 필수 입력값입니다.");
        }

        @Test
        void reportType이_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostReportValidator.validateReportType(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("신고 유형(reportType)은 필수 입력값입니다.");
        }

        @Test
        void reportType이_있으면_통과한다() {
            Assertions.assertThatCode(() -> PostReportValidator.validateReportType(PostReportType.BROKEN_LINK))
                      .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateContent")
    class ValidateContent {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void content가_비어있으면_BadRequestException을_던진다(String content) {
            Assertions.assertThatThrownBy(() -> PostReportValidator.validateContent(content))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("신고 사유는 비어있을 수 없습니다.");
        }

        @Test
        void content가_1000자를_초과하면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostReportValidator.validateContent("a".repeat(1001)))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("1000자를 초과할 수 없습니다.");
        }

        @Test
        void content가_유효하면_통과한다() {
            Assertions.assertThatCode(() -> PostReportValidator.validateContent("링크가 깨져 있습니다."))
                      .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateNoPendingReport")
    class ValidateNoPendingReport {

        @Test
        void 처리_대기중인_신고가_있으면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostReportValidator.validateNoPendingReport(true))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("이미 처리 대기 중인 게시글 신고가 있습니다.");
        }

        @Test
        void 처리_대기중인_신고가_없으면_통과한다() {
            Assertions.assertThatCode(() -> PostReportValidator.validateNoPendingReport(false))
                      .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateNewStatus")
    class ValidateNewStatus {

        @Test
        void newStatus가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostReportValidator.validateNewStatus(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("변경할 신고 상태는 필수 입력값입니다.");
        }

        @Test
        void newStatus가_PENDING이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostReportValidator.validateNewStatus(ReportStatus.PENDING))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("신고를 PENDING 으로 되돌릴 수 없습니다.");
        }

        @Test
        void newStatus가_PENDING이_아니면_통과한다() {
            Assertions.assertThatCode(() -> PostReportValidator.validateNewStatus(ReportStatus.REJECTED_KEEP))
                      .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getPostReportOrThrow")
    class GetPostReportOrThrow {

        @Test
        void 존재하면_PostReport를_반환한다() {
            PostReport report = PostReport.builder()
                                          .id(UUID.randomUUID())
                                          .reportStatus(ReportStatus.PENDING)
                                          .build();

            PostReport found = PostReportValidator.getPostReportOrThrow(report.id(), ignored -> Optional.of(report));

            Assertions.assertThat(found).isSameAs(report);
        }

        @Test
        void 없으면_NotFoundException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostReportValidator.getPostReportOrThrow(UUID.randomUUID(), ignored -> Optional.empty()))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("존재하지 않는 게시글 신고입니다.");
        }

        @Test
        void reportId가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostReportValidator.getPostReportOrThrow(null, ignored -> Optional.empty()))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("reportId는 필수 입력값입니다.");
        }
    }
}
