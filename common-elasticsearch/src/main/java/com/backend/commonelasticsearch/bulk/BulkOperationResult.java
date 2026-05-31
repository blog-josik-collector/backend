package com.backend.commonelasticsearch.bulk;

import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record BulkOperationResult(Set<UUID> failedIds, int successCount) {

    public static BulkOperationResult empty() {
        return new BulkOperationResult(Set.of(), 0);
    }

    public static BulkOperationResult of(BulkResponse response) {
        Set<UUID> failed = new HashSet<>();
        int success = 0;
        for (BulkResponseItem item : response.items()) {
            if (item.error() != null) {
                failed.add(UUID.fromString(item.id()));
            } else {
                success++;
            }
        }
        return new BulkOperationResult(Set.copyOf(failed), success);
    }

    public boolean isFailed(UUID id) {
        return failedIds.contains(id);
    }
}
