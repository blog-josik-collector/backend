package com.backend.interactionservice.post.service.dto;

public record PostViewCountFlushResult(int postCount, long totalIncrement) {

    public static PostViewCountFlushResult of(int postCount, long totalIncrement) {
        return new PostViewCountFlushResult(postCount, totalIncrement);
    }
}
