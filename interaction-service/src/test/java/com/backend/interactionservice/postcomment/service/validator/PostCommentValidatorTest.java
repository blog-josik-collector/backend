package com.backend.interactionservice.postcomment.service.validator;

import com.backend.commondataaccess.exception.AccessDeniedException;
import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.common.enums.PostCommentStatus;
import com.backend.commondataaccess.persistence.post.PostComment;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("PostCommentValidator 테스트")
class PostCommentValidatorTest {

    private static PostComment createComment(User user, PostComment parent) {
        return PostComment.builder()
                          .id(UUID.randomUUID())
                          .user(user)
                          .parentComment(parent)
                          .content("content")
                          .postCommentStatus(PostCommentStatus.ACTIVE)
                          .build();
    }

    private static User createUser() {
        return User.builder()
                   .id(UUID.randomUUID())
                   .userType(UserType.USER)
                   .nickname("nick")
                   .build();
    }

    @Nested
    @DisplayName("필수값 검증")
    class RequiredFields {

        @Test
        void userId가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostCommentValidator.validateUserId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("userId는 필수 입력값입니다.");
        }

        @Test
        void postId가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostCommentValidator.validatePostId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("postId는 필수 입력값입니다.");
        }

        @Test
        void commentId가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostCommentValidator.validateCommentId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("commentId는 필수 입력값입니다.");
        }
    }

    @Nested
    @DisplayName("validateContent")
    class ValidateContent {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void content가_비어있으면_BadRequestException을_던진다(String content) {
            Assertions.assertThatThrownBy(() -> PostCommentValidator.validateContent(content))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("댓글 내용은 비어있을 수 없습니다.");
        }

        @Test
        void content가_1000자를_초과하면_BadRequestException을_던진다() {
            String content = "a".repeat(1001);

            Assertions.assertThatThrownBy(() -> PostCommentValidator.validateContent(content))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("1000자를 초과할 수 없습니다.");
        }

        @Test
        void content가_1000자_이하면_통과한다() {
            String content = "a".repeat(1000);

            Assertions.assertThatCode(() -> PostCommentValidator.validateContent(content))
                      .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateOwnership")
    class ValidateOwnership {

        @Test
        void 작성자_본인이면_통과한다() {
            User user = createUser();
            PostComment comment = createComment(user, null);

            Assertions.assertThatCode(() -> PostCommentValidator.validateOwnership(comment, user.id()))
                      .doesNotThrowAnyException();
        }

        @Test
        void 작성자가_아니면_AccessDeniedException을_던진다() {
            PostComment comment = createComment(createUser(), null);

            Assertions.assertThatThrownBy(() -> PostCommentValidator.validateOwnership(comment, UUID.randomUUID()))
                      .isInstanceOf(AccessDeniedException.class)
                      .hasMessageContaining("댓글에 대한 권한이 없습니다.");
        }

        @Test
        void userId가_null이면_BadRequestException을_던진다() {
            PostComment comment = createComment(createUser(), null);

            Assertions.assertThatThrownBy(() -> PostCommentValidator.validateOwnership(comment, null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("userId는 필수 입력값입니다.");
        }
    }

    @Nested
    @DisplayName("validateIsComment / validateIsReply")
    class ValidateDepth {

        @Test
        void 댓글이면_validateIsComment를_통과한다() {
            PostComment comment = createComment(createUser(), null);

            Assertions.assertThatCode(() -> PostCommentValidator.validateIsComment(comment))
                      .doesNotThrowAnyException();
        }

        @Test
        void 대댓글이면_validateIsComment에서_BadRequestException을_던진다() {
            PostComment reply = createComment(createUser(), createComment(createUser(), null));

            Assertions.assertThatThrownBy(() -> PostCommentValidator.validateIsComment(reply))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("해당 리소스는 대댓글입니다.");
        }

        @Test
        void 대댓글이면_validateIsReply를_통과한다() {
            PostComment reply = createComment(createUser(), createComment(createUser(), null));

            Assertions.assertThatCode(() -> PostCommentValidator.validateIsReply(reply))
                      .doesNotThrowAnyException();
        }

        @Test
        void 댓글이면_validateIsReply에서_BadRequestException을_던진다() {
            PostComment comment = createComment(createUser(), null);

            Assertions.assertThatThrownBy(() -> PostCommentValidator.validateIsReply(comment))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("해당 리소스는 댓글입니다.");
        }
    }

    @Nested
    @DisplayName("getPostCommentOrThrow")
    class GetPostCommentOrThrow {

        @Test
        void 존재하면_PostComment를_반환한다() {
            PostComment comment = createComment(createUser(), null);

            PostComment found = PostCommentValidator.getPostCommentOrThrow(comment.id(), ignored -> Optional.of(comment));

            Assertions.assertThat(found).isSameAs(comment);
        }

        @Test
        void 없으면_NotFoundException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostCommentValidator.getPostCommentOrThrow(UUID.randomUUID(), ignored -> Optional.empty()))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("존재하지 않는 댓글입니다.");
        }

        @Test
        void commentId가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostCommentValidator.getPostCommentOrThrow(null, ignored -> Optional.empty()))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("commentId는 필수 입력값입니다.");
        }
    }
}
