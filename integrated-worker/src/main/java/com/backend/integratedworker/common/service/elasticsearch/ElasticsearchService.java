package com.backend.integratedworker.common.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.backend.integratedworker.common.service.elasticsearch.dto.BulkIndexResult;
import com.backend.integratedworker.common.service.elasticsearch.dto.EsPostDocument;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ElasticsearchService {

    private final String indexName;
    private final ElasticsearchClient elasticsearchClient;

    public ElasticsearchService(@Value("${elasticsearch.index-name}") String indexName,
                                ElasticsearchClient elasticsearchClient) {

        this.indexName = indexName;
        this.elasticsearchClient = elasticsearchClient;
    }

    public BulkIndexResult bulkIndex(List<EsPostDocument> documents) {
        if (documents.isEmpty()) {
            return BulkIndexResult.empty();
        }

        try {
            BulkRequest.Builder builder = new BulkRequest.Builder();
            for (EsPostDocument doc : documents) {
                builder.operations(op -> op
                        .index(idx -> idx
                                .index(indexName)
                                .id(doc.id().toString())
                                .document(doc)
                        )
                );
            }
            BulkResponse response = elasticsearchClient.bulk(builder.build());
            return BulkIndexResult.of(response);
        } catch (IOException e) {
            log.error("Elasticsearch bulk index failed", e);
            throw new IllegalStateException("ES bulk index failed", e);
        }
    }
}
