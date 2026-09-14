package com.backend.commonweb.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

class JacksonConfigTest {

    @Test
    void utc_OffsetDateTime을_KST_문자열로_직렬화한다() throws Exception {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().kstOffsetDateTimeCustomizer().customize(builder);
        ObjectMapper mapper = builder.build();

        OffsetDateTime utc = OffsetDateTime.of(2026, 9, 14, 14, 50, 49, 0, ZoneOffset.UTC);
        assertThat(mapper.writeValueAsString(utc)).isEqualTo("\"2026-09-14T23:50:49+09:00\"");
    }
}
