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
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchConfig {

    private static final int SHA256_FINGERPRINT_HEX_LENGTH = 64;

    @Bean
    public RestClient restClient(ElasticsearchProperties props) {
        HttpHost httpHost = new HttpHost(props.host(), props.port(), props.scheme());

        return RestClient.builder(httpHost)
                         .setHttpClientConfigCallback(httpClientBuilder -> {
                             // basic auth
                             if (StringUtils.hasText(props.username())) {
                                 BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
                                 credsProvider.setCredentials(
                                         AuthScope.ANY,
                                         new UsernamePasswordCredentials(props.username(), props.password()));
                                 httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
                             }
                             // TLS: CA fingerprint 검증 (https + 유효한 SHA-256 지문일 때만)
                             if ("https".equalsIgnoreCase(props.scheme())) {
                                 String fingerprint = props.fingerprint();
                                 if (StringUtils.hasText(fingerprint)) {
                                     if (!isValidCaFingerprint(fingerprint)) {
                                         throw new IllegalArgumentException(
                                                 "elasticsearch.fingerprint must be a 64-character SHA-256 hex string "
                                                         + "(optional colons). Check ELASTICSEARCH_FINGERPRINT.");
                                     }
                                     SSLContext sslContext = TransportUtils.sslContextFromCaFingerprint(fingerprint);
                                     httpClientBuilder.setSSLContext(sslContext);
                                 }
                             }
                             return httpClientBuilder;
                         })
                         .build();
    }

    private static boolean isValidCaFingerprint(String fingerprint) {
        String normalized = fingerprint.replace(":", "");
        if (normalized.length() != SHA256_FINGERPRINT_HEX_LENGTH) {
            return false;
        }
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
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
