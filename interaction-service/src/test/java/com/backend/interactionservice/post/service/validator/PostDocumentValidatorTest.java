package com.backend.interactionservice.post.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import com.backend.interactionservice.post.service.dto.PostDocument;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PostDocumentValidator 테스트")
class PostDocumentValidatorTest {

    @Nested
    @DisplayName("필수값 검증")
    class RequiredFields {

        @Test
        void id가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostDocumentValidator.validateId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("id는 필수 입력값입니다.");
        }
    }

    @Nested
    @DisplayName("getPostDocumentOrThrow")
    class GetPostDocumentOrThrow {

        @Test
        void 존재하면_PostDocument를_반환한다() {
            UUID id = UUID.randomUUID();
            PostDocument document = new PostDocument(id,
                                                     "title",
                                                     "https://test.com/1",
                                                     null,
                                                     "summary",
                                                     "Toss",
                                                     PostStatus.ACTIVE,
                                                     LocalDate.now(),
                                                     OffsetDateTime.now(),
                                                     OffsetDateTime.now(),
                                                     1,
                                                     2,
                                                     3,
                                                     0);

            PostDocument found = PostDocumentValidator.getPostDocumentOrThrow(id, ignored -> Optional.of(document));

            Assertions.assertThat(found).isSameAs(document);
        }

        @Test
        void 없으면_NotFoundException을_던진다() {
            UUID id = UUID.randomUUID();

            Assertions.assertThatThrownBy(() -> PostDocumentValidator.getPostDocumentOrThrow(id, ignored -> Optional.empty()))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("존재하지 않는 PostDocument입니다.");
        }

        @Test
        void id가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostDocumentValidator.getPostDocumentOrThrow(null, ignored -> Optional.empty()))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("id는 필수 입력값입니다.");
        }
    }
}
