package com.backend.commonelasticsearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.TransportUtils;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchConfig {

    @Bean
    public RestClient restClient(ElasticsearchProperties props) {
        HttpHost httpHost = new HttpHost(props.host(), props.port(), props.scheme());

        return RestClient.builder(httpHost)
                         .setHttpClientConfigCallback(httpClientBuilder -> {
                             // basic auth
                             if (props.username() != null && !props.username().isBlank()) {
                                 BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
                                 credsProvider.setCredentials(
                                         AuthScope.ANY,
                                         new UsernamePasswordCredentials(props.username(), props.password()));
                                 httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
                             }
                             // TLS: CA fingerprint 검증
                             if ("https".equalsIgnoreCase(props.scheme())
                                     && props.fingerprint() != null && !props.fingerprint().isBlank()) {
                                 SSLContext sslContext = TransportUtils.sslContextFromCaFingerprint(props.fingerprint());
                                 httpClientBuilder.setSSLContext(sslContext);
                             }
                             return httpClientBuilder;
                         })
                         .build();
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(objectMapper);
        return new RestClientTransport(restClient, jsonpMapper);
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }
}
