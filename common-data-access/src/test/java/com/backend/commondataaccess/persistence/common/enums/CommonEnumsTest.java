package com.backend.commondataaccess.persistence.common.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("공통 enum 테스트")
class CommonEnumsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("jsonValues")
    void JSON으로_직렬화하면_소문자_이름을_사용한다(Enum<?> value, String expected) throws Exception {
        Assertions.assertThat(objectMapper.writeValueAsString(value)).isEqualTo("\"" + expected + "\"");
    }

    @DisplayName("CollectScheduleType.from")
    @Nested
    class CollectScheduleTypeFromTest {

        @ParameterizedTest
        @ValueSource(strings = {"cron", "CRON", "Cron"})
        void 대소문자를_구분하지_않고_변환한다(String name) {
            Assertions.assertThat(CollectScheduleType.from(name)).isEqualTo(CollectScheduleType.CRON);
        }

        @Test
        void 없는_이름이면_null을_반환한다() {
            Assertions.assertThat(CollectScheduleType.from("unknown")).isNull();
        }

        @Test
        void JSON_역직렬화에_사용한다() throws Exception {
            Assertions.assertThat(objectMapper.readValue("\"manual\"", CollectScheduleType.class))
                      .isEqualTo(CollectScheduleType.MANUAL);
        }
    }

    @DisplayName("IndexingJobType.from")
    @Nested
    class IndexingJobTypeFromTest {

        @ParameterizedTest
        @ValueSource(strings = {"manual", "MANUAL", "Manual"})
        void 대소문자를_구분하지_않고_변환한다(String name) {
            Assertions.assertThat(IndexingJobType.from(name)).isEqualTo(IndexingJobType.MANUAL);
        }

        @Test
        void 없는_이름이면_null을_반환한다() {
            Assertions.assertThat(IndexingJobType.from("unknown")).isNull();
        }

        @Test
        void JSON_역직렬화에_사용한다() throws Exception {
            Assertions.assertThat(objectMapper.readValue("\"cron\"", IndexingJobType.class))
                      .isEqualTo(IndexingJobType.CRON);
        }
    }

    static Stream<Arguments> jsonValues() {
        return Stream.of(
                Arguments.of(CollectScheduleType.CRON, "cron"),
                Arguments.of(CollectingStatus.FETCH_FAILED, "fetch_failed"),
                Arguments.of(CommentReportType.POLITICAL, "political"),
                Arguments.of(IndexingJobType.MANUAL, "manual"),
                Arguments.of(IndexingStatus.SKIPPED, "skipped"),
                Arguments.of(JobStatus.CANCELLED, "cancelled"),
                Arguments.of(PostCommentStatus.BLOCKED, "blocked"),
                Arguments.of(PostReportType.BROKEN_LINK, "broken_link"),
                Arguments.of(PostStatus.DELETED, "deleted"),
                Arguments.of(ReportStatus.RESOLVED_DELETED, "resolved_deleted")
        );
    }
}
