package com.backend.commonelasticsearch.client;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

@FunctionalInterface
public interface ApplicationElasticsearchClientCallback<T> {

    T execute(ElasticsearchClient client) throws Exception;
}
