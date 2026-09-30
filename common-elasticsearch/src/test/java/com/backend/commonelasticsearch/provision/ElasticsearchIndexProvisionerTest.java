package com.backend.commonelasticsearch.provision;

import static org.mockito.ArgumentMatchers.any;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.ReindexRequest;
import co.elastic.clients.elasticsearch.core.ReindexResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsAliasRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesResponse;
import co.elastic.clients.elasticsearch.indices.get_alias.IndexAliases;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.backend.commondataaccess.exception.InfraException;
import com.backend.commonelasticsearch.client.ApplicationElasticsearchClient;
import com.backend.commonelasticsearch.config.ElasticsearchProperties;
import com.backend.commonelasticsearch.config.ElasticsearchProperties.Provisioning;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("ElasticsearchIndexProvisioner 테스트")
@ExtendWith(MockitoExtension.class)
class ElasticsearchIndexProvisionerTest {

    private static final String ALIAS = "techblog-posts";
    private static final String DEFINITION_LOCATION = "classpath:elasticsearch/techblog-posts.json";
    private static final int NUMBER_OF_REPLICAS = 0;
    private static final String PHYSICAL_INDEX_PATTERN = ALIAS + "-\\d{12}";
    private static final String DEFINITION_JSON = """
            {
              "settings": { "index": { "number_of_replicas": 0 } },
              "mappings": { "properties": { "title": { "type": "text" } } }
            }
            """;

    @Mock
    private ElasticsearchTransport transport;

    @Mock
    private IndexDefinitionAssembler indexDefinitionAssembler;

    private ElasticsearchIndexProvisioner elasticsearchIndexProvisioner;

    private final List<Object> requests = new ArrayList<>();

    private boolean aliasExists;

    private Supplier<List<String>> aliasedIndices;

    private Long reindexTotal;

    @BeforeEach
    void init() throws Exception {
        ElasticsearchProperties properties = new ElasticsearchProperties("localhost",
                                                                         9200,
                                                                         "http",
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         ALIAS,
                                                                         new Provisioning(true,
                                                                                          DEFINITION_LOCATION,
                                                                                          NUMBER_OF_REPLICAS));
        ApplicationElasticsearchClient applicationElasticsearchClient =
                new ApplicationElasticsearchClient(new ElasticsearchClient(transport));
        elasticsearchIndexProvisioner = new ElasticsearchIndexProvisioner(applicationElasticsearchClient,
                                                                          properties,
                                                                          indexDefinitionAssembler);

        aliasExists = false;
        aliasedIndices = List::of;
        reindexTotal = 0L;

        Mockito.lenient().doReturn(new JacksonJsonpMapper()).when(transport).jsonpMapper();
        Mockito.lenient().doReturn(DEFINITION_JSON)
               .when(indexDefinitionAssembler)
               .assemble(DEFINITION_LOCATION, NUMBER_OF_REPLICAS);
        Mockito.lenient().doAnswer(invocation -> respond(invocation.getArgument(0)))
               .when(transport)
               .performRequest(any(), any(), any());
    }

    private Object respond(Object request) {
        requests.add(request);
        if (request instanceof ExistsAliasRequest) {
            return new BooleanResponse(aliasExists);
        }
        if (request instanceof GetAliasRequest) {
            return GetAliasResponse.of(r -> {
                aliasedIndices.get().forEach(index -> r.result(index, IndexAliases.of(i -> i.aliases(Map.of()))));
                return r;
            });
        }
        if (request instanceof CreateIndexRequest createIndexRequest) {
            return CreateIndexResponse.of(r -> r.index(createIndexRequest.index())
                                                .acknowledged(true)
                                                .shardsAcknowledged(true));
        }
        if (request instanceof UpdateAliasesRequest) {
            return UpdateAliasesResponse.of(r -> r.acknowledged(true));
        }
        if (request instanceof ReindexRequest) {
            return ReindexResponse.of(r -> r.total(reindexTotal));
        }
        throw new IllegalStateException("unexpected request: " + request);
    }

    private <T> List<T> requestsOf(Class<T> type) {
        return requests.stream().filter(type::isInstance).map(type::cast).toList();
    }

    @Test
    void alias는_설정의_index_alias를_반환한다() {
        Assertions.assertThat(elasticsearchIndexProvisioner.alias()).isEqualTo(ALIAS);
    }

    @DisplayName("aliasExists 테스트")
    @Nested
    class AliasExistsTest {

        @Test
        void alias_존재_여부를_조회한다() {
            // given
            aliasExists = true;

            // when
            boolean result = elasticsearchIndexProvisioner.aliasExists();

            // then
            Assertions.assertThat(result).isTrue();
            Assertions.assertThat(requestsOf(ExistsAliasRequest.class))
                      .singleElement()
                      .satisfies(request -> Assertions.assertThat(request.name()).containsExactly(ALIAS));
        }
    }

    @DisplayName("currentIndex 테스트")
    @Nested
    class CurrentIndexTest {

        @Test
        void alias가_없으면_빈_값을_반환한다() {
            // when
            Optional<String> result = elasticsearchIndexProvisioner.currentIndex();

            // then
            Assertions.assertThat(result).isEmpty();
            Assertions.assertThat(requestsOf(GetAliasRequest.class)).isEmpty();
        }

        @Test
        void alias가_여러_인덱스를_가리키면_가장_최신_인덱스를_반환한다() {
            // given
            aliasExists = true;
            aliasedIndices = () -> List.of(ALIAS + "-250101000000", ALIAS + "-260101000000", ALIAS + "-251231235959");

            // when
            Optional<String> result = elasticsearchIndexProvisioner.currentIndex();

            // then
            Assertions.assertThat(result).contains(ALIAS + "-260101000000");
            Assertions.assertThat(requestsOf(GetAliasRequest.class))
                      .singleElement()
                      .satisfies(request -> Assertions.assertThat(request.name()).containsExactly(ALIAS));
        }
    }

    @DisplayName("bootstrapIfAbsent 테스트")
    @Nested
    class BootstrapIfAbsentTest {

        @Test
        void alias가_이미_있으면_생성하지_않는다() {
            // given
            aliasExists = true;
            aliasedIndices = () -> List.of(ALIAS + "-250101000000");

            // when
            ProvisionResult result = elasticsearchIndexProvisioner.bootstrapIfAbsent();

            // then
            Assertions.assertThat(result.alias()).isEqualTo(ALIAS);
            Assertions.assertThat(result.index()).isEqualTo(ALIAS + "-250101000000");
            Assertions.assertThat(result.created()).isFalse();
            Assertions.assertThat(requestsOf(CreateIndexRequest.class)).isEmpty();
            Assertions.assertThat(requestsOf(UpdateAliasesRequest.class)).isEmpty();
            Mockito.verifyNoInteractions(indexDefinitionAssembler);
        }

        @Test
        void alias가_없으면_물리_인덱스와_write_alias를_생성한다() {
            // when
            ProvisionResult result = elasticsearchIndexProvisioner.bootstrapIfAbsent();

            // then
            Assertions.assertThat(result.alias()).isEqualTo(ALIAS);
            Assertions.assertThat(result.index()).matches(PHYSICAL_INDEX_PATTERN);
            Assertions.assertThat(result.created()).isTrue();

            CreateIndexRequest createIndexRequest = requestsOf(CreateIndexRequest.class).get(0);
            Assertions.assertThat(createIndexRequest.index()).isEqualTo(result.index());
            Assertions.assertThat(createIndexRequest.mappings().properties()).containsKey("title");
            Assertions.assertThat(createIndexRequest.settings().index().numberOfReplicas()).isEqualTo("0");

            List<Action> actions = requestsOf(UpdateAliasesRequest.class).get(0).actions();
            Assertions.assertThat(actions).singleElement().satisfies(action -> {
                Assertions.assertThat(action.isAdd()).isTrue();
                Assertions.assertThat(action.add().index()).isEqualTo(result.index());
                Assertions.assertThat(action.add().alias()).isEqualTo(ALIAS);
                Assertions.assertThat(action.add().isWriteIndex()).isTrue();
            });
        }
    }

    @DisplayName("reindexToNewIndex 테스트")
    @Nested
    class ReindexToNewIndexTest {

        @Test
        void 원본_인덱스가_없으면_InfraException이_발생한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> elasticsearchIndexProvisioner.reindexToNewIndex())
                      .isInstanceOf(InfraException.class);
            Assertions.assertThat(requestsOf(CreateIndexRequest.class)).isEmpty();
        }

        @Test
        void 새_인덱스명이_원본과_같으면_InfraException이_발생한다() throws InterruptedException {
            // given
            aliasExists = true;
            aliasedIndices = () -> List.of(
                    ALIAS + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")));

            // 초 경계에 걸려 인덱스명이 달라지지 않도록 초 앞부분에서 실행한다.
            while (LocalTime.now().getNano() > 500_000_000) {
                Thread.sleep(10);
            }

            // when & then
            Assertions.assertThatThrownBy(() -> elasticsearchIndexProvisioner.reindexToNewIndex())
                      .isInstanceOf(InfraException.class);
            Assertions.assertThat(requestsOf(CreateIndexRequest.class)).isEmpty();
        }

        @Test
        void 새_인덱스로_재색인하고_alias를_원자적으로_스왑한다() {
            // given
            String sourceIndex = ALIAS + "-250101000000";
            aliasExists = true;
            aliasedIndices = () -> List.of(sourceIndex);
            reindexTotal = 42L;

            // when
            ReindexResult result = elasticsearchIndexProvisioner.reindexToNewIndex();

            // then
            Assertions.assertThat(result.alias()).isEqualTo(ALIAS);
            Assertions.assertThat(result.sourceIndex()).isEqualTo(sourceIndex);
            Assertions.assertThat(result.newIndex()).matches(PHYSICAL_INDEX_PATTERN).isNotEqualTo(sourceIndex);
            Assertions.assertThat(result.documents()).isEqualTo(42L);

            Assertions.assertThat(requestsOf(CreateIndexRequest.class))
                      .singleElement()
                      .satisfies(request -> Assertions.assertThat(request.index()).isEqualTo(result.newIndex()));

            ReindexRequest reindexRequest = requestsOf(ReindexRequest.class).get(0);
            Assertions.assertThat(reindexRequest.source().index()).containsExactly(sourceIndex);
            Assertions.assertThat(reindexRequest.dest().index()).isEqualTo(result.newIndex());
            Assertions.assertThat(reindexRequest.refresh()).isTrue();

            List<Action> actions = requestsOf(UpdateAliasesRequest.class).get(0).actions();
            Assertions.assertThat(actions).hasSize(2);
            Assertions.assertThat(actions.get(0).isRemove()).isTrue();
            Assertions.assertThat(actions.get(0).remove().index()).isEqualTo(sourceIndex);
            Assertions.assertThat(actions.get(0).remove().alias()).isEqualTo(ALIAS);
            Assertions.assertThat(actions.get(1).isAdd()).isTrue();
            Assertions.assertThat(actions.get(1).add().index()).isEqualTo(result.newIndex());
            Assertions.assertThat(actions.get(1).add().alias()).isEqualTo(ALIAS);
            Assertions.assertThat(actions.get(1).add().isWriteIndex()).isTrue();
        }

        @Test
        void 재색인_문서_수가_없으면_0으로_반환한다() {
            // given
            aliasExists = true;
            aliasedIndices = () -> List.of(ALIAS + "-250101000000");
            reindexTotal = null;

            // when
            ReindexResult result = elasticsearchIndexProvisioner.reindexToNewIndex();

            // then
            Assertions.assertThat(result.documents()).isZero();
        }
    }
}
