package com.backend.integratedworker.collectingjob.service.dto;

import java.time.LocalDate;
import java.util.Optional;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Getter
@SuperBuilder
public abstract class Post {

    protected String title;
    protected String url;
    protected LocalDate publishedAt; // 발행일
    protected Optional<String> thumbnailUrl;  // 썸네일 이미지
    protected Optional<String> summary; // 요약

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("title", title)
                .append("url", url)
                .append("publishedAt", publishedAt)
                .append("thumbnailUrl", thumbnailUrl.isEmpty() ? "" : thumbnailUrl.get())
                .append("summary", summary.isEmpty() ? "" : summary.get())
                .toString();
    }
}
