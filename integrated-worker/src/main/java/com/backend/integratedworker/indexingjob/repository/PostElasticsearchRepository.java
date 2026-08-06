package com.backend.integratedworker.indexingjob.repository;

import com.backend.commonelasticsearch.operation.bulk.BulkOperationResult;
import com.backend.commonelasticsearch.operation.bulk.ElasticsearchBulkOperations;
import com.backend.commonelasticsearch.client.ApplicationElasticsearchClient;
import com.backend.integratedworker.indexingjob.repository.dto.EsPostDocument;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * techblog-posts 인덱스에 포스팅 문서를 bulk index 하는 Elasticsearch repository.
 */
@Repository
public class PostElasticsearchRepository {

    private final ElasticsearchBulkOperations bulkOperations;

    public PostElasticsearchRepository(ApplicationElasticsearchClient applicationElasticsearchClient,
                                       @Value("${elasticsearch.index-alias}") String indexAlias) {

        this.bulkOperations = new ElasticsearchBulkOperations(applicationElasticsearchClient, indexAlias);
    }

    public BulkOperationResult bulkIndex(List<EsPostDocument> documents) {
        if (documents.isEmpty()) {
            return BulkOperationResult.empty();
        }

        return bulkOperations.bulkIndex(documents, doc -> doc.id().toString());
    }
}
