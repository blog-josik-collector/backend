package com.backend.interactionservice.post.repository.query;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchCondition {
    private String title;
    private String provider;
}
