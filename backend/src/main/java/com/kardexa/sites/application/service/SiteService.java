package com.kardexa.sites.application.service;

import com.kardexa.sites.application.port.in.ChangeSiteStatusUseCase;
import com.kardexa.sites.application.port.in.CreateSiteUseCase;
import com.kardexa.sites.application.port.in.DeleteSiteUseCase;
import com.kardexa.sites.application.port.in.GetSiteUseCase;
import com.kardexa.sites.application.port.in.ListSitesUseCase;
import com.kardexa.sites.application.port.in.UpdateSiteUseCase;
import com.kardexa.shared.domain.MainSite;
import com.kardexa.sites.application.port.out.SiteRepository;
import com.kardexa.sites.domain.exception.DuplicateSiteNameException;
import com.kardexa.sites.domain.exception.LastActiveSiteException;
import com.kardexa.sites.domain.exception.SiteNotDeletableException;
import com.kardexa.sites.domain.exception.SiteNotFoundException;
import com.kardexa.sites.domain.model.Site;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Casos de uso de HU-19 sobre las sedes. Java puro: se registra como bean en {@code SitesConfig}. */
public class SiteService
        implements CreateSiteUseCase, UpdateSiteUseCase, ChangeSiteStatusUseCase, DeleteSiteUseCase, ListSitesUseCase,
                GetSiteUseCase {

    private final SiteRepository sites;

    public SiteService(SiteRepository sites) {
        this.sites = Objects.requireNonNull(sites);
    }

    @Override
    public Site create(CreateSiteCommand command) {
        Site site = Site.create(new Site.Details(command.name(), command.address()));
        if (sites.existsByName(site.name())) {
            throw new DuplicateSiteNameException(site.name());
        }
        return sites.save(site);
    }

    @Override
    public Site update(UpdateSiteCommand command) {
        Site site = find(command.id());
        site.update(new Site.Details(command.name(), command.address()));
        if (sites.existsByNameExcluding(site.name(), site.id())) {
            throw new DuplicateSiteNameException(site.name());
        }
        return sites.save(site);
    }

    @Override
    public Site changeStatus(UUID id, boolean active) {
        Site site = find(id);
        if (active) {
            site.activate();
        } else {
            if (site.isActive() && sites.findAllOrderedByName().stream().filter(Site::isActive).count() <= 1) {
                throw new LastActiveSiteException();
            }
            site.deactivate();
        }
        return sites.save(site);
    }

    @Override
    public void delete(UUID id) {
        Site site = find(id);
        if (MainSite.ID.equals(id)) {
            throw SiteNotDeletableException.main();
        }
        if (site.isActive() && sites.findAllOrderedByName().stream().filter(Site::isActive).count() <= 1) {
            throw new LastActiveSiteException();
        }
        sites.delete(id);
    }

    @Override
    public Site get(UUID id) {
        return find(id);
    }

    @Override
    public List<Site> list() {
        return sites.findAllOrderedByName();
    }

    private Site find(UUID id) {
        return sites.findById(id).orElseThrow(() -> new SiteNotFoundException(id));
    }
}
