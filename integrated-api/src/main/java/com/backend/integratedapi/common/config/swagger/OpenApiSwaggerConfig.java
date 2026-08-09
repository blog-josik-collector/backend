package com.backend.integratedapi.common.config.swagger;

import com.backend.commondataaccess.security.CurrentUser;
import com.backend.commonweb.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

@OpenAPIDefinition(
        tags = {
                @Tag(name = "01. Provider 관리 API"),
                @Tag(name = "02. 수집 소스 관리 API"),
                @Tag(name = "02. 수집 작업 실행 API"),
                @Tag(name = "03. 수집 결과 조회 API"),
                @Tag(name = "03. 색인 작업 실행 API"),
                @Tag(name = "04. 색인된 Post 문서 조회 API"),
                @Tag(name = "05. Elasticsearch 인덱스 관리 API")
        }
)
@Configuration
public class OpenApiSwaggerConfig {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final String ERROR_SCHEMA = "ErrorResponse";

    static {
        // springdoc은 @AuthenticationPrincipal만 기본 무시하므로, 커스텀 애노테이션은 직접 등록해야 문서에 노출되지 않는다.
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
    }

    @Bean
    public ModelResolver modelResolver(ObjectMapper objectMapper) {
        // Jackson의 네이밍 전략을 스네이크 케이스로 설정한 ObjectMapper를 ModelResolver에 주입
        return new ModelResolver(objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE));
    }

    @Bean
    public OpenAPI IntegratedServiceOpenApi() {
        Schema<?> errorSchema = ModelConverters.getInstance()
                                               .readAllAsResolvedSchema(ErrorResponse.class)
                .schema;
        return new OpenAPI()
                .info(new Info().title("Integrated-Service API")
                                .description("integrated-service REST API")
                                .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                                    .addSchemas(ERROR_SCHEMA, errorSchema)
                                    .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                            .type(SecurityScheme.Type.HTTP)
                                            .scheme("bearer")
                                            .bearerFormat("JWT")
                                            .description("user-service 로그인 또는 구글 OAuth 콜백에서 발급받은 access token")));
    }

    @Bean
    public GlobalOperationCustomizer commonResponseCustomizer() {
        return (operation, handlerMethod) -> {
            operation.getResponses()
                     .addApiResponse("400", errorResponse("요청 값이 올바르지 않음", "FE40001", "입력 데이터에 문제가 있습니다.", 400))
                     .addApiResponse("500", errorResponse("처리되지 않은 서버 오류", "FE50001", "서버 처리 오류(관리자에게 문의하세요).", 500));
            if (isOptionalAuth(handlerMethod)) {
                operation.setSecurity(List.of());
            } else {
                operation.getResponses()
                         .addApiResponse("401", errorResponse("토큰이 없거나 유효하지 않음", "BE40101", "인증되지 않은 사용자입니다.", 401))
                         .addApiResponse("403", errorResponse("권한이 없는 사용자", "BE40301", "권한이 없는 사용자입니다.", 403));
            }
            return operation;
        };
    }


    @Bean
    public GroupedOpenApi IntegratedV1Api() {
        return GroupedOpenApi.builder()
                             .group("integrated-v1")
                             .pathsToMatch("/collect/v1/**", "/index/v1/**")
                             .build();
    }

    private boolean isOptionalAuth(HandlerMethod handlerMethod) {
        return Arrays.stream(handlerMethod.getMethodParameters())
                     .map(parameter -> parameter.getParameterAnnotation(CurrentUser.class))
                     .filter(Objects::nonNull)
                     .anyMatch(currentUser -> !currentUser.required());
    }

    private ApiResponse errorResponse(String description, String code, String message, int status) {
        Schema<?> ref = new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA);
        Map<String, Object> example = Map.of(
                "code", code,
                "message", message,
                "status", status,
                "timestamp", "2026-08-09T12:00:00+09:00"
        );
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(
                        "application/json",
                        new MediaType().schema(ref).example(example)));
    }
}
