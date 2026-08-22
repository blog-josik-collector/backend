package com.backend.commonweb.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.backend.commondataaccess.persistence.common.enums.PostReportType;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import org.junit.jupiter.api.Test;

class JsonValueEnumConverterFactoryTest {

    private final JsonValueEnumConverterFactory factory = new JsonValueEnumConverterFactory();

    @Test
    void jsonValue_형식으로_enum을_변환한다() {
        var converter = factory.getConverter(PostReportType.class);

        assertThat(converter.convert("invalid_content")).isEqualTo(PostReportType.INVALID_CONTENT);
        assertThat(converter.convert("broken_link")).isEqualTo(PostReportType.BROKEN_LINK);
    }

    @Test
    void enum_이름으로도_enum을_변환한다() {
        var converter = factory.getConverter(ReportStatus.class);

        assertThat(converter.convert("PENDING")).isEqualTo(ReportStatus.PENDING);
        assertThat(converter.convert("pending")).isEqualTo(ReportStatus.PENDING);
        assertThat(converter.convert("resolved_deleted")).isEqualTo(ReportStatus.RESOLVED_DELETED);
    }

    @Test
    void 잘못된_값이면_IllegalArgumentException을_던진다() {
        var converter = factory.getConverter(PostReportType.class);

        assertThatThrownBy(() -> converter.convert("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PostReportType");
    }
}
