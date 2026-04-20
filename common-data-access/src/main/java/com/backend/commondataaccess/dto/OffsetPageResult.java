package com.backend.commondataaccess.dto;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OffsetPageResult<T> {

    private long totalCount;
    private int page;
    private int size;
    private List<T> items;

    public boolean hasNext() {
        return page + size < totalCount;
    }

    public <R> OffsetPageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mappedItems = items.stream()
                                     .map(mapper)
                                     .collect(Collectors.toList());
        return new OffsetPageResult<>(totalCount, page, size, mappedItems);
    }
}
