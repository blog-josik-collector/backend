package com.backend.integratedworker.collectingjob.service.crawler.strategy;

import com.backend.integratedworker.collectingjob.service.dto.Post;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public interface PostParser<T extends Post> {

    DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    T parse(Object rawData);

    LocalDate parsePublishedAt(String metaData);
}
