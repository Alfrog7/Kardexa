package com.kardexa.sites.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.sites.application.port.in.CreateSiteUseCase.CreateSiteCommand;
import com.kardexa.sites.application.port.in.UpdateSiteUseCase.UpdateSiteCommand;
import com.kardexa.sites.domain.exception.DuplicateSiteNameException;
import com.kardexa.sites.domain.exception.LastActiveSiteException;
import com.kardexa.sites.domain.exception.SiteNotFoundException;
import com.kardexa.sites.domain.model.Site;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Casos de uso de sedes (HU-19)")
class SiteServiceTest {

    private InMemorySiteRepository repository;
    private SiteService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySiteRepository();
        service = new SiteService(repository);
    }

    private Site create(String name) {
        return service.create(new CreateSiteCommand(name, "Huanchaco"));
    }

    @Test
    @DisplayName("una sede nueva queda registrada y habilitada")
    void createsSite() {
        Site created = create("Huanchaco");

        assertThat(repository.findById(created.id())).isPresent();
        assertThat(created.isActive()).isTrue();
    }

    @Test
    @DisplayName("criterio 1: no se repite el nombre, sin distinguir mayúsculas ni espacios")
    void rejectsDuplicateName() {
        create("Galeno");

        assertThatThrownBy(() -> create("  GALENO "))
                .isInstanceOf(DuplicateSiteNameException.class);
    }

    @Test
    @DisplayName("editar cambia el nombre y la dirección, y rechaza el nombre de otra sede")
    void updates() {
        Site galeno = create("Galeno");
        Site monserrate = create("Monserrate");

        Site updated = service.update(new UpdateSiteCommand(galeno.id(), "Galeno Norte", "Av. España 100"));
        assertThat(updated.name()).isEqualTo("Galeno Norte");
        assertThat(updated.details().address()).isEqualTo("Av. España 100");

        assertThatThrownBy(() -> service.update(new UpdateSiteCommand(monserrate.id(), "galeno norte", null)))
                .isInstanceOf(DuplicateSiteNameException.class);
        assertThat(service.update(new UpdateSiteCommand(monserrate.id(), "MONSERRATE", null)).name())
                .isEqualTo("MONSERRATE");
    }

    @Test
    @DisplayName("criterio 2: inhabilitar conserva la sede y se puede volver a habilitar")
    void changesStatus() {
        Site site = create("Galeno");
        create("Monserrate");

        assertThat(service.changeStatus(site.id(), false).isActive()).isFalse();
        assertThat(service.list()).hasSize(2);
        assertThat(service.changeStatus(site.id(), true).isActive()).isTrue();
    }

    @Test
    @DisplayName("la última sede habilitada no se puede inhabilitar")
    void keepsOneActiveSite() {
        Site galeno = create("Galeno");
        Site monserrate = create("Monserrate");
        service.changeStatus(galeno.id(), false);

        assertThatThrownBy(() -> service.changeStatus(monserrate.id(), false))
                .isInstanceOf(LastActiveSiteException.class);
        assertThat(service.changeStatus(galeno.id(), false).isActive()).isFalse();
    }

    @Test
    @DisplayName("una sede inexistente responde no encontrada")
    void notFound() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.update(new UpdateSiteCommand(unknown, "Nada", null)))
                .isInstanceOf(SiteNotFoundException.class);
        assertThatThrownBy(() -> service.changeStatus(unknown, false)).isInstanceOf(SiteNotFoundException.class);
    }

    @Test
    @DisplayName("la lista va por nombre sin distinguir mayúsculas e incluye las inhabilitadas")
    void listsOrdered() {
        create("monserrate");
        Site galeno = create("Galeno");
        service.changeStatus(galeno.id(), false);

        assertThat(service.list()).extracting(Site::name).containsExactly("Galeno", "monserrate");
    }
}
