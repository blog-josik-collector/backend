package com.backend.commondataaccess.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "오프셋 기반 페이징 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OffsetPageResult<T> {

    @Schema(description = "전체 건수", example = "95")
    private long totalCount;

    @Schema(description = "현재 페이지 번호(0부터 시작)", example = "0")
    private int page;

    @Schema(description = "페이지 크기", example = "20")
    private int size;

    @Schema(description = "현재 페이지 항목 목록")
    private List<T> items;

    public boolean hasNext() {
        return ((long) page * size) < totalCount;
    }

    public <R> OffsetPageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mappedItems = items.stream()
                                     .map(mapper)
                                     .collect(Collectors.toList());
        return new OffsetPageResult<>(totalCount, page, size, mappedItems);
    }
}
