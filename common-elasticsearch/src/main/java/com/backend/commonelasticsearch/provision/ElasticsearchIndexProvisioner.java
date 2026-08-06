package com.backend.commonelasticsearch.provision;

import co.elastic.clients.elasticsearch.core.ReindexResponse;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.json.JsonpMapper;
import com.backend.commondataaccess.exception.ErrorCode;
import com.backend.commondataaccess.exception.InfraException;
import com.backend.commonelasticsearch.client.ApplicationElasticsearchClient;
import com.backend.commonelasticsearch.config.ElasticsearchProperties;
import com.backend.commonelasticsearch.operation.ElasticsearchOperation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.stream.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * 물리 인덱스 생성 / alias 연결 / 재색인 + 원자적 alias 스왑을 담당하는 프로비저너. <br>
 * - 애플리케이션은 alias({@code elasticsearch.index-alias})만 바라보고, 물리 인덱스명은 이 클래스가 관리한다. <br>
 * - 물리 인덱스명 규칙: {@code <alias>-<yyMMddHHmmss>} (예: {@code techblog-posts-260806142530}). <br>
 * - 매핑/세팅 정의는 {@code elasticsearch.provisioning.definition-location} JSON 을 단일 소스로 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexProvisioner {

    private static final DateTimeFormatter INDEX_TIMESTAMP = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final String REPLICAS_FIELD = "number_of_replicas";

    private final ApplicationElasticsearchClient applicationElasticsearchClient;
    private final ElasticsearchProperties properties;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    /**
     * 애플리케이션이 사용하는 alias 이름.
     */
    public String alias() {
        return properties.indexAlias();
    }

    /**
     * alias 가 없으면 물리 인덱스 + write alias 를 생성한다. 이미 있으면 그대로 둔다(idempotent).
     */
    public ProvisionResult bootstrapIfAbsent() {
        String alias = alias();
        if (aliasExists()) {
            return new ProvisionResult(alias, currentIndex().orElse(null), false);
        }

        String physicalIndex = generatePhysicalIndexName();
        createPhysicalIndex(physicalIndex);
        addWriteAlias(physicalIndex);
        log.info("[ES] bootstrap created index={} alias={}", physicalIndex, alias);
        return new ProvisionResult(alias, physicalIndex, true);
    }

    /**
     * 새 물리 인덱스를 정의 JSON 으로 생성하고, 현재 alias 대상 인덱스에서 재색인한 뒤, alias 를 원자적으로 스왑한다. <br>
     * 이전 인덱스는 삭제하지 않고 남겨두어 롤백/검증에 활용한다.
     */
    public ReindexResult reindexToNewIndex() {
        String alias = alias();
        String sourceIndex = currentIndex().orElseThrow(() -> new InfraException(
                ErrorCode.IE_ELASTICSEARCH_ERROR, "재색인 원본이 없습니다. alias=" + alias));

        String newIndex = generatePhysicalIndexName();
        if (newIndex.equals(sourceIndex)) {
            throw new InfraException(ErrorCode.IE_ELASTICSEARCH_ERROR,
                                     "새 인덱스명이 원본과 동일합니다(1초 내 재실행). 잠시 후 다시 시도하세요. index=" + newIndex);
        }

        createPhysicalIndex(newIndex);
        long documents = reindex(sourceIndex, newIndex);
        swapAlias(sourceIndex, newIndex);
        log.info("[ES] reindex done alias={} {} -> {} documents={}", alias, sourceIndex, newIndex, documents);
        return new ReindexResult(alias, sourceIndex, newIndex, documents);
    }

    /**
     * alias 가 현재 가리키는 물리 인덱스. 다중 연결(비정상)일 경우 이름순 최신(=최근 timestamp) 을 반환한다.
     */
    public Optional<String> currentIndex() {
        if (!aliasExists()) {
            return Optional.empty();
        }
        GetAliasResponse response = applicationElasticsearchClient.execute(
                ElasticsearchOperation.GET_ALIAS,
                client -> client.indices().getAlias(a -> a.name(alias())));

        return response.result().keySet().stream().max(Comparator.naturalOrder());
    }

    public boolean aliasExists() {
        return applicationElasticsearchClient.execute(
                ElasticsearchOperation.EXISTS,
                client -> client.indices().existsAlias(a -> a.name(alias())).value());
    }

    private String generatePhysicalIndexName() {
        return alias() + "-" + LocalDateTime.now().format(INDEX_TIMESTAMP);
    }

    private void createPhysicalIndex(String indexName) {
        String body = definitionJsonWithReplicaOverride();
        applicationElasticsearchClient.execute(ElasticsearchOperation.CREATE_INDEX, client -> {
            JsonpMapper mapper = client._transport().jsonpMapper();
            try (InputStream in = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))) {
                JsonParser parser = mapper.jsonProvider().createParser(in);
                return client.indices().create(c -> c.index(indexName).withJson(parser, mapper));
            }
        });
    }

    private void addWriteAlias(String indexName) {
        applicationElasticsearchClient.execute(ElasticsearchOperation.UPDATE_ALIASES, client ->
                client.indices().updateAliases(u -> u.actions(a -> a.add(add -> add.index(indexName)
                                                                                    .alias(alias())
                                                                                    .isWriteIndex(true)))));
    }

    private void swapAlias(String oldIndex, String newIndex) {
        applicationElasticsearchClient.execute(ElasticsearchOperation.UPDATE_ALIASES, client ->
                client.indices().updateAliases(u -> u
                        .actions(a -> a.remove(r -> r.index(oldIndex).alias(alias())))
                        .actions(a -> a.add(add -> add.index(newIndex).alias(alias()).isWriteIndex(true)))));
    }

    private long reindex(String sourceIndex, String destIndex) {
        ReindexResponse response = applicationElasticsearchClient.execute(
                ElasticsearchOperation.REINDEX,
                client -> client.reindex(r -> r.source(s -> s.index(sourceIndex))
                                               .dest(d -> d.index(destIndex))
                                               .refresh(true)));
        return response.total() != null ? response.total() : 0L;
    }

    /**
     * 정의 JSON 을 읽어 {@code settings.index.number_of_replicas} 를 환경별 설정값으로 덮어쓴 문자열을 반환한다.
     */
    private String definitionJsonWithReplicaOverride() {
        Resource resource = resourceLoader.getResource(properties.provisioning().definitionLocation());
        try (InputStream in = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            JsonNode indexNode = root.path("settings").path("index");
            if (indexNode instanceof ObjectNode objectNode) {
                objectNode.put(REPLICAS_FIELD, properties.provisioning().numberOfReplicas());
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new InfraException(ErrorCode.IE_ELASTICSEARCH_ERROR,
                                     "인덱스 정의 로드 실패: " + properties.provisioning().definitionLocation(), e);
        }
    }
}
