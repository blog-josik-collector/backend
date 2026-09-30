package com.backend.commonelasticsearch.client;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.ErrorCode;
import com.backend.commondataaccess.exception.InfraException;
import com.backend.commonelasticsearch.operation.ElasticsearchOperation;
import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("ApplicationElasticsearchClient 테스트")
@ExtendWith(MockitoExtension.class)
class ApplicationElasticsearchClientTest {

    @InjectMocks
    private ApplicationElasticsearchClient applicationElasticsearchClient;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Test
    void callback에_ElasticsearchClient를_전달하고_결과를_반환한다() {
        // when
        ElasticsearchClient result = applicationElasticsearchClient.execute(ElasticsearchOperation.GET,
                                                                            client -> client);

        // then
        Assertions.assertThat(result).isSameAs(elasticsearchClient);
    }

    @Test
    void BusinessException은_변환하지_않고_그대로_던진다() {
        // given
        BadRequestException exception = new BadRequestException("test_message");

        // when & then
        Assertions.assertThatThrownBy(() -> applicationElasticsearchClient.execute(ElasticsearchOperation.GET,
                                                                                    client -> {
                                                                                        throw exception;
                                                                                    }))
                  .isSameAs(exception);
    }

    @Test
    void IOException은_InfraException으로_변환해_던진다() {
        // given
        IOException exception = new IOException("connection refused");

        // when & then
        Assertions.assertThatThrownBy(() -> applicationElasticsearchClient.execute(ElasticsearchOperation.SEARCH,
                                                                                    client -> {
                                                                                        throw exception;
                                                                                    }))
                  .isInstanceOf(InfraException.class)
                  .hasCause(exception)
                  .extracting("errorCode")
                  .isEqualTo(ErrorCode.IE_ELASTICSEARCH_ERROR);
    }

    @Test
    void 요청_오류_ElasticsearchException은_BadRequestException으로_변환해_던진다() {
        // given
        ElasticsearchException exception = new ElasticsearchException(
                "test_endpoint",
                ErrorResponse.of(r -> r.status(400).error(e -> e.type("mapper_parsing_exception"))));

        // when & then
        Assertions.assertThatThrownBy(() -> applicationElasticsearchClient.execute(ElasticsearchOperation.BULK,
                                                                                    client -> {
                                                                                        throw exception;
                                                                                    }))
                  .isInstanceOf(BadRequestException.class);
    }
}
