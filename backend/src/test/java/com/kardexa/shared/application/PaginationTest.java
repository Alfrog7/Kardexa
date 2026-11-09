package com.kardexa.shared.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Paginación de los casos de uso de consulta")
class PaginationTest {

    @Test
    @DisplayName("sin valores usa la primera página y el tamaño por defecto")
    void defaults() {
        assertThat(PageQuery.of(null, null)).isEqualTo(new PageQuery(0, PageQuery.DEFAULT_SIZE));
        assertThat(new PageQuery(2, 10).offset()).isEqualTo(20);
    }

    @ParameterizedTest(name = "página {0}, tamaño {1}")
    @CsvSource({"-1, 20", "0, 0", "0, 101"})
    @DisplayName("rechaza página negativa o tamaño fuera de 1..100")
    void rejectsOutOfRange(int page, int size) {
        assertThatThrownBy(() -> new PageQuery(page, size)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "{0} elementos de a {1} → {2} páginas")
    @CsvSource({"0, 20, 0", "1, 20, 1", "20, 20, 1", "21, 20, 2"})
    @DisplayName("calcula el total de páginas")
    void totalPages(long total, int size, int pages) {
        assertThat(new ResultPage<>(List.of(), 0, size, total).totalPages()).isEqualTo(pages);
    }

    @Test
    @DisplayName("transforma el contenido y conserva los datos de la página")
    void maps() {
        ResultPage<Integer> page = ResultPage.of(List.of(1, 2), new PageQuery(1, 2), 5);

        assertThat(page.map(n -> "n" + n)).isEqualTo(new ResultPage<>(List.of("n1", "n2"), 1, 2, 5));
    }

    @Test
    @DisplayName("el total no puede ser menor que el contenido")
    void rejectsInconsistentTotal() {
        assertThatThrownBy(() -> new ResultPage<>(List.of(1, 2), 0, 20, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
