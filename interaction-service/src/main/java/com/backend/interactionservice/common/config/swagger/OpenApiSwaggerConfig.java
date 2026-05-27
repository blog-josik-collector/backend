package com.backend.interactionservice.common.config.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        tags = {
                @Tag(name = "01. 포스팅 조회 API"),
                @Tag(name = "02. 포스팅 좋아요 API"),
                @Tag(name = "03. 포스팅 즐겨찾기 API"),
                @Tag(name = "04. 포스팅 댓글/대댓글 API")
        }
)
@Configuration
public class OpenApiSwaggerConfig {

    @Bean
    public ModelResolver modelResolver(ObjectMapper objectMapper) {
        // Jackson의 네이밍 전략을 스네이크 케이스로 설정한 ObjectMapper를 ModelResolver에 주입
        return new ModelResolver(objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE));
    }

    @Bean
    public OpenAPI userServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Interaction Service API")
                        .description("interaction-service REST API")
                        .version("v1"));
    }

    @Bean
    public GroupedOpenApi userV1Api() {
        return GroupedOpenApi.builder()
                .group("interaction-v1")
                .pathsToMatch("/interaction/v1/**")
                .build();
    }
}
