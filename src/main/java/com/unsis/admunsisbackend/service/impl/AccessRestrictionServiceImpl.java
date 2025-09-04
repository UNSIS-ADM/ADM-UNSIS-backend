package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.AccessRestrictionDTO;
import com.unsis.admunsisbackend.model.AccessRestriction;
import com.unsis.admunsisbackend.repository.AccessRestrictionRepository;
import com.unsis.admunsisbackend.service.AccessRestrictionService;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Optional;

@Service
public class AccessRestrictionServiceImpl implements AccessRestrictionService {

    private static final String ROLE_APPLICANT = "ROLE_APPLICANT";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";

    private final AccessRestrictionRepository repo;
    private final Clock clock;

    // Constructor: inyecta repo y acepta clock opcional (fallback a
    // systemDefaultZone)
    public AccessRestrictionServiceImpl(AccessRestrictionRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = (clock == null) ? Clock.systemDefaultZone() : clock;
    }

    @Override
    public boolean isAccessAllowed(String roleName) {
        if (ROLE_ADMIN.equals(roleName) || ROLE_USER.equals(roleName))
            return true;
        if (!ROLE_APPLICANT.equals(roleName))
            return true;

        Optional<AccessRestriction> opt = repo.findFirstByRoleName(ROLE_APPLICANT);
        if (opt.isEmpty())
            return true; // sin regla => permitir

        AccessRestriction rule = opt.get();

        // NUEVO: si la regla está deshabilitada -> DENEGAR
        if (!rule.isEnabled()) {
            return false;
        }

        LocalDate nowDate = LocalDate.now(clock);
        LocalTime nowTime = LocalTime.now(clock);

        LocalDate activationDate = rule.getActivationDate();
        LocalTime activationTime = rule.getActivationTime();

        boolean dateDefined = activationDate != null;
        boolean timeDefined = activationTime != null;

        if (dateDefined && timeDefined) {
            LocalDateTime now = LocalDateTime.of(nowDate, nowTime);
            LocalDateTime activation = LocalDateTime.of(activationDate, activationTime);
            return !now.isBefore(activation);
        } else if (dateDefined) {
            return !nowDate.isBefore(activationDate);
        } else if (timeDefined) {
            return !nowTime.isBefore(activationTime);
        } else {
            // habilitada pero sin moment configurado -> permitir
            return true;
        }
    }

    @Override
    public AccessRestriction getRestriction() {
        // devuelve la primera regla para ROLE_APPLICANT o null
        return repo.findFirstByRoleName(ROLE_APPLICANT).orElse(null);
    }

    @Override
    public AccessRestriction saveOrUpdate(AccessRestriction restriction) {
        // Aseguramos roleName por defecto si el DTO/cliente no lo envió
        if (restriction.getRoleName() == null || restriction.getRoleName().isBlank()) {
            restriction.setRoleName(ROLE_APPLICANT);
        }

        Optional<AccessRestriction> existing = repo.findFirstByRoleName(ROLE_APPLICANT);
        if (existing.isPresent()) {
            // si ya existe, utilizar su id (actualización)
            restriction.setId(existing.get().getId());
        }
        return repo.save(restriction);
    }

    // DTO helpers
    public static AccessRestrictionDTO toDto(AccessRestriction e) {
        AccessRestrictionDTO d = new AccessRestrictionDTO();
        if (e == null) {
            d.setRoleName(ROLE_APPLICANT);
            d.setActivationDate(null);
            d.setActivationTime(null);
            d.setEnabled(false);
            d.setDescription(null);
            d.setId(null);
            return d;
        }
        d.setId(e.getId());
        d.setRoleName(e.getRoleName());
        d.setActivationDate(e.getActivationDate() != null ? e.getActivationDate().toString() : null);
        d.setActivationTime(e.getActivationTime() != null ? e.getActivationTime().toString() : null);
        d.setEnabled(e.isEnabled());
        d.setDescription(e.getDescription());
        return d;
    }

    public static AccessRestriction fromDto(AccessRestrictionDTO d) {
        AccessRestriction e = new AccessRestriction();
        e.setId(d.getId());
        e.setRoleName(d.getRoleName() != null ? d.getRoleName() : ROLE_APPLICANT);
        e.setEnabled(d.isEnabled());
        e.setDescription(d.getDescription());
        if (d.getActivationDate() != null && !d.getActivationDate().isBlank()) {
            e.setActivationDate(LocalDate.parse(d.getActivationDate()));
        } else {
            e.setActivationDate(null);
        }
        if (d.getActivationTime() != null && !d.getActivationTime().isBlank()) {
            e.setActivationTime(LocalTime.parse(d.getActivationTime()));
        } else {
            e.setActivationTime(null);
        }
        return e;
    }

}
