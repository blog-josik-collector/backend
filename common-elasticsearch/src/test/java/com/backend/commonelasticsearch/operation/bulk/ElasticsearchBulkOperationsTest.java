package com.backend.commonelasticsearch.operation.bulk;

import static org.mockito.ArgumentMatchers.any;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.OperationType;
import co.elastic.clients.transport.ElasticsearchTransport;
import com.backend.commondataaccess.exception.InfraException;
import com.backend.commonelasticsearch.client.ApplicationElasticsearchClient;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("ElasticsearchBulkOperations 테스트")
@ExtendWith(MockitoExtension.class)
class ElasticsearchBulkOperationsTest {

    private static final String INDEX_NAME = "test-index";

    @Mock
    private ElasticsearchTransport transport;

    private ElasticsearchBulkOperations elasticsearchBulkOperations;

    private TestDocument firstDocument;

    private TestDocument secondDocument;

    @BeforeEach
    void init() {
        ApplicationElasticsearchClient applicationElasticsearchClient =
                new ApplicationElasticsearchClient(new ElasticsearchClient(transport));
        elasticsearchBulkOperations = new ElasticsearchBulkOperations(applicationElasticsearchClient, INDEX_NAME);

        firstDocument = new TestDocument(UUID.randomUUID(), "first_title");
        secondDocument = new TestDocument(UUID.randomUUID(), "second_title");
    }

    private static BulkResponseItem successItem(OperationType operationType, UUID id) {
        return BulkResponseItem.of(i -> i.operationType(operationType)
                                         .index(INDEX_NAME)
                                         .id(id.toString())
                                         .status(200));
    }

    private static BulkResponseItem failedItem(OperationType operationType, UUID id) {
        return BulkResponseItem.of(i -> i.operationType(operationType)
                                         .index(INDEX_NAME)
                                         .id(id.toString())
                                         .status(400)
                                         .error(e -> e.type("mapper_parsing_exception").reason("test_reason")));
    }

    private BulkRequest captureBulkRequest() throws IOException {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(transport).performRequest(captor.capture(), any(), any());
        return (BulkRequest) captor.getValue();
    }

    @DisplayName("bulkIndex 테스트")
    @Nested
    class BulkIndexTest {

        @Test
        void 문서가_없으면_요청하지_않고_빈_결과를_반환한다() {
            // when
            BulkOperationResult result = elasticsearchBulkOperations.bulkIndex(List.<TestDocument>of(),
                                                                               document -> document.id().toString());

            // then
            Assertions.assertThat(result.failedIds()).isEmpty();
            Assertions.assertThat(result.successCount()).isZero();
            Mockito.verifyNoInteractions(transport);
        }

        @Test
        void 문서_목록을_index_요청으로_보내고_성공_실패를_집계한다() throws IOException {
            // given
            BulkResponse response = BulkResponse.of(b -> b.errors(true)
                                                          .took(1L)
                                                          .items(successItem(OperationType.Index, firstDocument.id()),
                                                                 failedItem(OperationType.Index, secondDocument.id())));

            Mockito.doReturn(response).when(transport).performRequest(any(), any(), any());

            // when
            BulkOperationResult result = elasticsearchBulkOperations.bulkIndex(
                    List.of(firstDocument, secondDocument), document -> document.id().toString());

            // then
            Assertions.assertThat(result.successCount()).isEqualTo(1);
            Assertions.assertThat(result.failedIds()).containsExactly(secondDocument.id());
            Assertions.assertThat(result.isFailed(secondDocument.id())).isTrue();
            Assertions.assertThat(result.isFailed(firstDocument.id())).isFalse();

            List<BulkOperation> operations = captureBulkRequest().operations();
            Assertions.assertThat(operations).hasSize(2);
            Assertions.assertThat(operations).allMatch(BulkOperation::isIndex);
            Assertions.assertThat(operations.get(0).index().index()).isEqualTo(INDEX_NAME);
            Assertions.assertThat(operations.get(0).index().id()).isEqualTo(firstDocument.id().toString());
            Assertions.assertThat(operations.get(0).index().document()).isEqualTo(firstDocument);
            Assertions.assertThat(operations.get(1).index().id()).isEqualTo(secondDocument.id().toString());
            Assertions.assertThat(operations.get(1).index().document()).isEqualTo(secondDocument);
        }

        @Test
        void 통신에_실패하면_InfraException이_발생한다() throws IOException {
            // given
            Mockito.doThrow(new IOException("connection refused"))
                   .when(transport)
                   .performRequest(any(), any(), any());

            // when & then
            Assertions.assertThatThrownBy(() -> elasticsearchBulkOperations.bulkIndex(
                              List.of(firstDocument), document -> document.id().toString()))
                      .isInstanceOf(InfraException.class);
        }
    }

    @DisplayName("bulkUpdate 테스트")
    @Nested
    class BulkUpdateTest {

        @Test
        void 원본이_없으면_요청하지_않고_빈_결과를_반환한다() {
            // when
            BulkOperationResult result = elasticsearchBulkOperations.bulkUpdate(List.<TestDocument>of(),
                                                                                document -> document.id().toString(),
                                                                                TestDocument::title,
                                                                                true);

            // then
            Assertions.assertThat(result.failedIds()).isEmpty();
            Assertions.assertThat(result.successCount()).isZero();
            Mockito.verifyNoInteractions(transport);
        }

        @Test
        void 원본_목록을_부분_문서_update_요청으로_보내고_결과를_집계한다() throws IOException {
            // given
            BulkResponse response = BulkResponse.of(b -> b.errors(false)
                                                          .took(1L)
                                                          .items(successItem(OperationType.Update, firstDocument.id()),
                                                                 successItem(OperationType.Update,
                                                                             secondDocument.id())));

            Mockito.doReturn(response).when(transport).performRequest(any(), any(), any());

            // when
            BulkOperationResult result = elasticsearchBulkOperations.bulkUpdate(
                    List.of(firstDocument, secondDocument),
                    document -> document.id().toString(),
                    document -> new TestPartialDocument(document.title()),
                    true);

            // then
            Assertions.assertThat(result.successCount()).isEqualTo(2);
            Assertions.assertThat(result.failedIds()).isEmpty();

            List<BulkOperation> operations = captureBulkRequest().operations();
            Assertions.assertThat(operations).hasSize(2);
            Assertions.assertThat(operations).allMatch(BulkOperation::isUpdate);
            Assertions.assertThat(operations.get(0).update().index()).isEqualTo(INDEX_NAME);
            Assertions.assertThat(operations.get(0).update().id()).isEqualTo(firstDocument.id().toString());
            Assertions.assertThat(operations.get(0).update().action().doc())
                      .isEqualTo(new TestPartialDocument("first_title"));
            Assertions.assertThat(operations.get(0).update().action().docAsUpsert()).isTrue();
            Assertions.assertThat(operations.get(1).update().id()).isEqualTo(secondDocument.id().toString());
        }
    }

    private record TestDocument(UUID id, String title) {

    }

    private record TestPartialDocument(String title) {

    }
}
