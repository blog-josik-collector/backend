package com.backend.integratedworker.common.service.elasticsearch.dto;

import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BulkIndexResult {

    private final Set<UUID> failedIds;
    private final int successCount;

    public static BulkIndexResult of(BulkResponse response) {
        Set<UUID> failed = new HashSet<>();
        int success = 0;
        for (BulkResponseItem item : response.items()) {
            if (item.error() != null) {
                failed.add(UUID.fromString(item.id()));
            } else {
                success++;
            }
        }
        return new BulkIndexResult(failed, success);
    }

    public boolean isFailed(UUID id) {
        return failedIds.contains(id);
    }

    public static BulkIndexResult empty() {
        return new BulkIndexResult(Set.of(), 0);
    }
}
