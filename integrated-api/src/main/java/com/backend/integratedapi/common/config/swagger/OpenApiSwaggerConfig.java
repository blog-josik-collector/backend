package com.backend.integratedapi.common.config.swagger;

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
                @Tag(name = "01. Provider 관리 API")
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
    public OpenAPI IntegratedServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                              .title("Integrated-Service API")
                              .description("integrated-service REST API")
                              .version("v1"));
    }

    @Bean
    public GroupedOpenApi IntegratedV1Api() {
        return GroupedOpenApi.builder()
                             .group("integrated-v1")
                             .pathsToMatch("/collect/v1/**")
                             .build();
    }
}
