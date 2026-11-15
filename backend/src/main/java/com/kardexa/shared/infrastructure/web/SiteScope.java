package com.kardexa.shared.infrastructure.web;

import com.kardexa.shared.domain.MainSite;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Decide en qué sede opera una petición (ADR 0013, RN-13).
 *
 * <ul>
 *   <li>El {@code EMPLOYEE} opera en la sede de su token e ignora la que pida; mientras el token no traiga
 *       sede (HU-20), en la «Sede principal».</li>
 *   <li>El {@code ADMIN} indica la sede con {@code siteId}; sin ella, la «Sede principal».</li>
 * </ul>
 */
@Component
public class SiteScope {

    /** La sede en la que se registra o se consulta un solo kárdex. */
    public UUID single(Jwt jwt, UUID requested) {
        if (isAdmin(jwt)) {
            return requested != null ? requested : MainSite.ID;
        }
        return ownSite(jwt);
    }

    /**
     * La sede de una consulta que admite «todas»: el empleado ve la suya; el administrador, la que pida o,
     * sin {@code siteId}, todas (resultado nulo).
     */
    public UUID filter(Jwt jwt, UUID requested) {
        return isAdmin(jwt) ? requested : ownSite(jwt);
    }

    private static boolean isAdmin(Jwt jwt) {
        return "ADMIN".equals(jwt.getClaimAsString("role"));
    }

    private static UUID ownSite(Jwt jwt) {
        String claim = jwt.getClaimAsString("siteId");
        return claim == null ? MainSite.ID : UUID.fromString(claim);
    }
}
