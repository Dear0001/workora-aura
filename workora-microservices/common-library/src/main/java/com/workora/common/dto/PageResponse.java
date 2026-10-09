package com.workora.common.dto;

import java.util.List;

/**
 * Stable pagination envelope shared across services so clients never depend on
 * the Spring Data {@code Page} serialization shape.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages, page >= totalPages - 1);
    }
}
