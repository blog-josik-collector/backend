package com.backend.commonelasticsearch.provision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.backend.commondataaccess.exception.InfraException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class IndexDefinitionAssemblerTest {

    private IndexDefinitionAssembler assembler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        assembler = new IndexDefinitionAssembler(new DefaultResourceLoader(), objectMapper);
    }

    @Test
    void assemble_injectsDictionarySynonyms_andOverridesReplicas() throws Exception {
        String json = assembler.assemble("classpath:elasticsearch/techblog-posts.json", 0);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.path("settings").path("index").path("number_of_replicas").asInt()).isEqualTo(0);

        JsonNode rules = root.at("/settings/analysis/tokenizer/nori_tech_tokenizer/user_dictionary_rules");
        assertThat(rules.isArray()).isTrue();
        assertThat(rules.size()).isGreaterThan(10);
        assertThat(rules.toString()).contains("스프링부트");
        assertThat(root.at("/settings/analysis/tokenizer/nori_tech_tokenizer/user_dictionary_rules_file")
                       .isMissingNode()).isTrue();

        JsonNode synonyms = root.at("/settings/analysis/filter/tech_synonym_filter/synonyms");
        assertThat(synonyms.isArray()).isTrue();
        assertThat(synonyms.size()).isGreaterThan(10);
        assertThat(synonyms.toString()).contains("kubernetes");
        assertThat(root.at("/settings/analysis/filter/tech_synonym_filter/synonyms_file").isMissingNode()).isTrue();

        assertThat(root.at("/mappings/properties/title/analyzer").asText()).isEqualTo("tech_blog_analyzer");
        assertThat(root.at("/mappings/properties/title/search_analyzer").asText())
                .isEqualTo("tech_blog_search_analyzer");
        assertThat(root.at("/mappings/properties/summary/search_analyzer").asText())
                .isEqualTo("tech_blog_search_analyzer");
        assertThat(root.at("/settings/analysis/analyzer/tech_blog_search_analyzer/filter").toString())
                .contains("tech_synonym_filter");
        assertThat(root.at("/settings/analysis/analyzer/tech_blog_analyzer/filter").toString())
                .doesNotContain("tech_synonym_filter");
    }

    @Test
    void assemble_missingDictionaryFile_throws() {
        assertThatThrownBy(() -> assembler.assemble("classpath:elasticsearch/missing-definition.json", 0))
                .isInstanceOf(InfraException.class);
    }

    @Test
    void siblingLocation_resolvesRelativeToDefinition() {
        assertThat(IndexDefinitionAssembler.siblingLocation(
                "classpath:elasticsearch/techblog-posts.json", "techblog-synonyms.txt"))
                .isEqualTo("classpath:elasticsearch/techblog-synonyms.txt");
    }
}
