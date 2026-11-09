package com.kardexa.shared.application;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Una página de resultados con el total de elementos, como el esquema {@code PageMeta} del contrato. */
public record ResultPage<T>(List<T> content, int page, int size, long totalElements) {

    public ResultPage {
        content = List.copyOf(Objects.requireNonNull(content));
        if (totalElements < content.size()) {
            throw new IllegalArgumentException("El total no puede ser menor que el contenido de la página.");
        }
    }

    public static <T> ResultPage<T> of(List<T> content, PageQuery query, long totalElements) {
        return new ResultPage<>(content, query.page(), query.size(), totalElements);
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }

    public <R> ResultPage<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new ResultPage<>(mapped, page, size, totalElements);
    }
}
