package com.backend.interactionservice.post.service.dto;

public record PostCountSyncResult(int totalPosts, int successCount, int failedCount) {

    public static PostCountSyncResult skipped() {
        return new PostCountSyncResult(0, 0, 0);
    }
}
