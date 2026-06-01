package com.backend.commonelasticsearch.operation.bulk;

import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.backend.commonelasticsearch.client.ApplicationElasticsearchClient;
import com.backend.commonelasticsearch.operation.ElasticsearchOperation;
import java.util.List;
import java.util.function.Function;

public final class ElasticsearchBulkOperations {

    private final ApplicationElasticsearchClient applicationElasticsearchClient;
    private final String indexName;

    public ElasticsearchBulkOperations(ApplicationElasticsearchClient applicationElasticsearchClient, String indexName) {
        this.applicationElasticsearchClient = applicationElasticsearchClient;
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
        return executeBulk(builder.build());
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
        return executeBulk(builder.build());
    }

    private BulkOperationResult executeBulk(BulkRequest request) {
        return applicationElasticsearchClient.execute(ElasticsearchOperation.BULK, client -> {
            BulkResponse response = client.bulk(request);
            return BulkOperationResult.of(response);
        });
    }
}
