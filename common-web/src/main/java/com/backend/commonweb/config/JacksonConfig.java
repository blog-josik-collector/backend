package com.backend.commonweb.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API OffsetDateTime 응답 오프셋을 Asia/Seoul(+09:00)로 통일.
 * DB timestamptz / JDBC 가 UTC(Z) 로 읽어도 순간(instant)은 유지한 채 오프셋만 바꾼다.
 */
@Configuration
public class JacksonConfig {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer kstOffsetDateTimeCustomizer() {
        return builder -> builder.serializerByType(OffsetDateTime.class, new JsonSerializer<OffsetDateTime>() {
            @Override
            public void serialize(OffsetDateTime value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException {
                gen.writeString(value.atZoneSameInstant(KST).toOffsetDateTime().toString());
            }
        });
    }
}
