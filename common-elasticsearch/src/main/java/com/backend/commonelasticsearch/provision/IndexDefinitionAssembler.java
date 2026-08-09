package com.backend.commonelasticsearch.provision;

import com.backend.commondataaccess.exception.ErrorCode;
import com.backend.commondataaccess.exception.InfraException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * 인덱스 정의 JSON 과 분리된 사전/동의어 파일을 합쳐 ES create-index body 를 만든다. <br>
 * JSON 안의 {@code user_dictionary_rules_file} / {@code synonyms_file} 은 정의 파일과 같은 디렉터리의
 * 상대 경로로 해석한 뒤, 각각 {@code user_dictionary_rules} / {@code synonyms} 배열로 치환한다.
 */
@Component
@RequiredArgsConstructor
public class IndexDefinitionAssembler {

    private static final String REPLICAS_FIELD = "number_of_replicas";
    private static final String USER_DICTIONARY_RULES_FILE = "user_dictionary_rules_file";
    private static final String USER_DICTIONARY_RULES = "user_dictionary_rules";
    private static final String SYNONYMS_FILE = "synonyms_file";
    private static final String SYNONYMS = "synonyms";

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    /**
     * 정의 JSON 을 읽고 사전/동의어 파일을 주입한 뒤, 복제본 수를 환경값으로 덮어쓴다.
     */
    public String assemble(String definitionLocation, int numberOfReplicas) {
        Resource definition = resourceLoader.getResource(definitionLocation);
        try (InputStream in = definition.getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            expandAnalysisFileReferences(root, definitionLocation);

            JsonNode indexNode = root.path("settings").path("index");
            if (indexNode instanceof ObjectNode objectNode) {
                objectNode.put(REPLICAS_FIELD, numberOfReplicas);
            }
            return objectMapper.writeValueAsString(root);
        } catch (InfraException e) {
            throw e;
        } catch (Exception e) {
            throw new InfraException(ErrorCode.IE_ELASTICSEARCH_ERROR,
                                     "인덱스 정의 로드 실패: " + definitionLocation, e);
        }
    }

    private void expandAnalysisFileReferences(JsonNode root, String definitionLocation) {
        JsonNode analysis = root.path("settings").path("analysis");
        if (analysis.isMissingNode()) {
            return;
        }
        expandInChildren(analysis.path("tokenizer"), definitionLocation,
                         USER_DICTIONARY_RULES_FILE, USER_DICTIONARY_RULES);
        expandInChildren(analysis.path("filter"), definitionLocation,
                         SYNONYMS_FILE, SYNONYMS);
    }

    private void expandInChildren(JsonNode parent,
                                  String definitionLocation,
                                  String fileField,
                                  String targetField) {
        if (!(parent instanceof ObjectNode parentObject)) {
            return;
        }
        for (Map.Entry<String, JsonNode> entry : parentObject.properties()) {
            if (!(entry.getValue() instanceof ObjectNode component)) {
                continue;
            }
            JsonNode fileNode = component.get(fileField);
            if (fileNode == null || !fileNode.isTextual()) {
                continue;
            }
            List<String> lines = readDictionaryLines(siblingLocation(definitionLocation, fileNode.asText()));
            ArrayNode array = objectMapper.createArrayNode();
            lines.forEach(array::add);
            component.set(targetField, array);
            component.remove(fileField);
        }
    }

    private List<String> readDictionaryLines(String location) {
        Resource resource = resourceLoader.getResource(location);
        try (InputStream in = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                lines.add(trimmed);
            }
            if (lines.isEmpty()) {
                throw new InfraException(ErrorCode.IE_ELASTICSEARCH_ERROR, "사전/동의어 파일이 비어 있습니다: " + location);
            }
            return lines;
        } catch (InfraException e) {
            throw e;
        } catch (Exception e) {
            throw new InfraException(ErrorCode.IE_ELASTICSEARCH_ERROR, "사전/동의어 파일 로드 실패: " + location, e);
        }
    }

    static String siblingLocation(String definitionLocation, String relativeFileName) {
        String fileName = relativeFileName.trim();
        if (fileName.contains("://") || fileName.startsWith("classpath:") || fileName.startsWith("file:")) {
            return fileName;
        }
        int slash = Math.max(definitionLocation.lastIndexOf('/'), definitionLocation.lastIndexOf('\\'));
        if (slash < 0) {
            return fileName;
        }
        return definitionLocation.substring(0, slash + 1) + fileName;
    }
}
