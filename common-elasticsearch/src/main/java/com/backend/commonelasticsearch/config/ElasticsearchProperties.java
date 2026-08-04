package com.backend.commonelasticsearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "elasticsearch")
public record ElasticsearchProperties(String host,
                                      int port,
                                      String scheme,
                                      String username,
                                      String password,
                                      String fingerprint,
                                      String indexName) {

}
