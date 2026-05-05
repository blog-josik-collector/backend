package com.backend.integratedworker.common.config.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.host:#{null}}")
    private String host;

    @Value("${elasticsearch.port:#{null}}")
    private int port;

    @Bean
    public RestClient restClient() {
        return RestClient.builder(new HttpHost(host, port))
                         .build();
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        // 1. LocalDate를 지원하는 ObjectMapper 생성 및 모듈 등록
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // 날짜를 배열[2024, 5, 5]이 아닌 문자열"2024-05-05"로 직렬화
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 2. 생성한 ObjectMapper를 JacksonJsonpMapper에 주입
        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(objectMapper);

        return new RestClientTransport(restClient, jsonpMapper);
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }
}
