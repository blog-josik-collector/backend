package com.backend.interactionservice.post.service.dto;

public record PostCountSyncResult(int totalPosts, int successCount, int failedCount) {

}
