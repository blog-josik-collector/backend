package com.backend.interactionservice.post.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.backend.commondataaccess.persistence.post.Post;
import com.backend.commonelasticsearch.bulk.BulkOperationResult;
import com.backend.commonelasticsearch.bulk.ElasticsearchBulkOperations;
import com.backend.interactionservice.post.repository.dto.PostCountFields;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * posts 테이블의 카운트 필드를 Elasticsearch techblog-posts-v1 인덱스에 bulk partial upsert 한다. <br>
 * 문서가 없으면 카운트 필드만으로 생성(doc_as_upsert)하고, 있으면 해당 필드만 병합(update)한다.
 */
@Repository
public class PostCountsElasticsearchRepository {

    private final ElasticsearchBulkOperations bulkOperations;

    public PostCountsElasticsearchRepository(ElasticsearchClient elasticsearchClient,
                                             @Value("${elasticsearch.index-name}") String indexName) {

        this.bulkOperations = new ElasticsearchBulkOperations(elasticsearchClient, indexName);
    }

    public BulkOperationResult bulkUpsertCounts(List<Post> posts) {
        if (posts.isEmpty()) {
            return BulkOperationResult.empty();
        }

        return bulkOperations.bulkUpdate(posts,
                                       post -> post.id().toString(),
                                       PostCountFields::from,
                                       true);
    }
}
