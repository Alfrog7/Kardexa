package com.kardexa.sites.infrastructure.adapter.in.web;

import com.kardexa.sites.application.port.in.ChangeSiteStatusUseCase;
import com.kardexa.sites.application.port.in.CreateSiteUseCase;
import com.kardexa.sites.application.port.in.CreateSiteUseCase.CreateSiteCommand;
import com.kardexa.sites.application.port.in.DeleteSiteUseCase;
import com.kardexa.sites.application.port.in.GetSiteUseCase;
import com.kardexa.sites.application.port.in.ListSitesUseCase;
import com.kardexa.sites.application.port.in.UpdateSiteUseCase;
import com.kardexa.sites.application.port.in.UpdateSiteUseCase.UpdateSiteCommand;
import com.kardexa.sites.domain.model.Site;
import com.kardexa.shared.infrastructure.web.SiteScope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** {@code /api/v1/sites} según el contrato {@code docs/api/openapi.yaml} (HU-19). Solo {@code ADMIN}, salvo {@code /mine}. */
@RestController
@RequestMapping("/sites")
class SiteController {

    private final CreateSiteUseCase createSite;
    private final UpdateSiteUseCase updateSite;
    private final ChangeSiteStatusUseCase changeStatus;
    private final DeleteSiteUseCase deleteSite;
    private final ListSitesUseCase listSites;
    private final GetSiteUseCase getSite;
    private final SiteScope siteScope;

    SiteController(CreateSiteUseCase createSite, UpdateSiteUseCase updateSite,
            ChangeSiteStatusUseCase changeStatus, DeleteSiteUseCase deleteSite, ListSitesUseCase listSites,
            GetSiteUseCase getSite, SiteScope siteScope) {
        this.createSite = createSite;
        this.updateSite = updateSite;
        this.changeStatus = changeStatus;
        this.deleteSite = deleteSite;
        this.listSites = listSites;
        this.getSite = getSite;
        this.siteScope = siteScope;
    }

    @GetMapping
    List<SiteResponse> list() {
        return listSites.list().stream().map(SiteResponse::from).toList();
    }

    /** La sede de quien consulta: la de su token si es trabajadora; la «Sede principal» si es ADMIN. */
    @GetMapping("/mine")
    SiteResponse mine(@AuthenticationPrincipal Jwt jwt) {
        return SiteResponse.from(getSite.get(siteScope.single(jwt, null)));
    }

    @PostMapping
    ResponseEntity<SiteResponse> create(@Valid @RequestBody SiteRequest request) {
        Site created = createSite.create(new CreateSiteCommand(request.name(), request.address()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(SiteResponse.from(created));
    }

    @PutMapping("/{id}")
    SiteResponse update(@PathVariable UUID id, @Valid @RequestBody SiteRequest request) {
        return SiteResponse.from(updateSite.update(new UpdateSiteCommand(id, request.name(), request.address())));
    }

    @PatchMapping("/{id}")
    SiteResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody SiteStatusRequest request) {
        return SiteResponse.from(changeStatus.changeStatus(id, request.active()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        deleteSite.delete(id);
    }

    /** Esquema {@code SiteRequest}: la dirección es opcional. */
    record SiteRequest(
            @NotBlank(message = "El nombre es obligatorio.")
            @Size(min = Site.NAME_MIN_LENGTH, max = Site.NAME_MAX_LENGTH,
                    message = "Debe tener entre {min} y {max} caracteres.")
            String name,
            @Size(max = Site.ADDRESS_MAX_LENGTH, message = "Admite hasta {max} caracteres.")
            String address) {}

    /** Esquema {@code SiteStatusRequest}. */
    record SiteStatusRequest(@NotNull(message = "Indica si la sede está habilitada.") Boolean active) {}

    /** Esquema {@code Site}. */
    record SiteResponse(UUID id, String name, String address, boolean active) {

        static SiteResponse from(Site site) {
            return new SiteResponse(site.id(), site.name(), site.details().address(), site.isActive());
        }
    }
}
