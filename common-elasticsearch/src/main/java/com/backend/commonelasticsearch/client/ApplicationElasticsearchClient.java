package com.backend.commonelasticsearch.client;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.backend.commondataaccess.exception.BusinessException;
import com.backend.commonelasticsearch.exception.ElasticsearchExceptionTranslator;
import com.backend.commonelasticsearch.operation.ElasticsearchOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 코드에서 주입해 사용하는 Elasticsearch client. <br> co.elastic.clients.elasticsearch.ElasticsearchClient 를 감싸 호출마다 예외 변환을 적용한다. <br> ES 를 사용하는 모듈은 {@link ElasticsearchExceptionTranslator} 를 직접 호출하지 않고
 * 이 클래스만 사용한다.
 */
@Component
@RequiredArgsConstructor
public class ApplicationElasticsearchClient {

    private final ElasticsearchClient elasticsearchClient;

    public <T> T execute(ElasticsearchOperation operation, ApplicationElasticsearchClientCallback<T> callback) {
        try {
            return callback.execute(elasticsearchClient);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw ElasticsearchExceptionTranslator.translate(operation, e);
        }
    }
}
