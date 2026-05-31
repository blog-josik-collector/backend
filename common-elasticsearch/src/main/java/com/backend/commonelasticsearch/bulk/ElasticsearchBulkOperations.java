package com.backend.commonelasticsearch.bulk;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ElasticsearchBulkOperations {

    private final ElasticsearchClient client;
    private final String indexName;

    public ElasticsearchBulkOperations(ElasticsearchClient client, String indexName) {
        this.client = client;
        this.indexName = indexName;
    }

    public <T> BulkOperationResult bulkIndex(List<T> documents, Function<T, String> idExtractor) {
        if (documents.isEmpty()) {
            return BulkOperationResult.empty();
        }

        BulkRequest.Builder builder = new BulkRequest.Builder();
        for (T document : documents) {
            builder.operations(op -> op.index(idx -> idx.index(indexName)
                                                          .id(idExtractor.apply(document))
                                                          .document(document)));
        }
        return executeBulk(builder.build(), "ES bulk index failed");
    }

    public <T> BulkOperationResult bulkUpdate(List<T> documents,
                                              Function<T, String> idExtractor,
                                              boolean docAsUpsert) {
        return bulkUpdate(documents, idExtractor, Function.identity(), docAsUpsert);
    }

    public <S, T> BulkOperationResult bulkUpdate(List<S> sources,
                                                 Function<S, String> idExtractor,
                                                 Function<S, T> docMapper,
                                                 boolean docAsUpsert) {
        if (sources.isEmpty()) {
            return BulkOperationResult.empty();
        }

        BulkRequest.Builder builder = new BulkRequest.Builder();
        for (S source : sources) {
            T document = docMapper.apply(source);
            builder.operations(op -> op.update(u -> u.index(indexName)
                                                     .id(idExtractor.apply(source))
                                                     .action(a -> a.doc(document)
                                                                   .docAsUpsert(docAsUpsert))));
        }
        return executeBulk(builder.build(), "ES bulk update failed");
    }

    private BulkOperationResult executeBulk(BulkRequest request, String errorMessage) {
        try {
            BulkResponse response = client.bulk(request);
            return BulkOperationResult.of(response);
        } catch (IOException e) {
            log.error(errorMessage, e);
            throw new IllegalStateException(errorMessage, e);
        }
    }
}
