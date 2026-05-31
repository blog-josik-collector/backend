package com.backend.integratedworker.indexingjob.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.backend.commonelasticsearch.bulk.BulkOperationResult;
import com.backend.commonelasticsearch.bulk.ElasticsearchBulkOperations;
import com.backend.integratedworker.indexingjob.repository.dto.EsPostDocument;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * techblog-posts-v1 인덱스에 포스팅 문서를 bulk index 하는 Elasticsearch repository.
 */
@Repository
public class PostElasticsearchRepository {

    private final ElasticsearchBulkOperations bulkOperations;

    public PostElasticsearchRepository(ElasticsearchClient elasticsearchClient,
                                       @Value("${elasticsearch.index-name}") String indexName) {

        this.bulkOperations = new ElasticsearchBulkOperations(elasticsearchClient, indexName);
    }

    public BulkOperationResult bulkIndex(List<EsPostDocument> documents) {
        if (documents.isEmpty()) {
            return BulkOperationResult.empty();
        }

        return bulkOperations.bulkIndex(documents, doc -> doc.id().toString());
    }
}
