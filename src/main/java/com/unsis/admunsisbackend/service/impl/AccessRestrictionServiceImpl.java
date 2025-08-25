package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.AccessRestrictionDTO;
import com.unsis.admunsisbackend.model.AccessRestriction;
import com.unsis.admunsisbackend.repository.AccessRestrictionRepository;
import com.unsis.admunsisbackend.service.AccessRestrictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Optional;

@Service
public class AccessRestrictionServiceImpl implements AccessRestrictionService {

    private static final String ROLE_APPLICANT = "ROLE_APPLICANT";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";

    @Autowired
    private AccessRestrictionRepository repo;

    // Clock para testabilidad / zona (usa zona del servidor por defecto)
    private final Clock clock = Clock.systemDefaultZone();

    @Override
    public boolean isAccessAllowed(String roleName) {
        // Roles sin restricción
        if (ROLE_ADMIN.equals(roleName) || ROLE_USER.equals(roleName)) {
            return true;
        }
        if (!ROLE_APPLICANT.equals(roleName)) {
            return true;
        }

        Optional<AccessRestriction> opt = repo.findFirstByRoleName(ROLE_APPLICANT);
        if (opt.isEmpty()) {
            return true; // sin regla configurada => permitir
        }

        AccessRestriction rule = opt.get();

        // defensivo: si está deshabilitada -> permitir
        if (!rule.isEnabled()) {
            return true;
        }

        LocalDate nowDate = LocalDate.now(clock);
        LocalTime nowTime = LocalTime.now(clock);

        LocalDate startDate = rule.getStartDate();
        LocalDate endDate = rule.getEndDate();
        LocalTime startTime = rule.getStartTime();
        LocalTime endTime = rule.getEndTime();

        boolean dateDefined = startDate != null && endDate != null;
        boolean timeDefined = startTime != null && endTime != null;

        boolean inDateRange = false;
        if (dateDefined) {
            inDateRange = !nowDate.isBefore(startDate) && !nowDate.isAfter(endDate);
        }

        boolean inTimeRange = false;
        if (timeDefined) {
            if (!startTime.isAfter(endTime)) {
                // rango normal (ej. 09:00 - 17:00)
                inTimeRange = !nowTime.isBefore(startTime) && !nowTime.isAfter(endTime);
            } else {
                // rango que cruza medianoche (ej. 22:00 - 02:00)
                inTimeRange = !nowTime.isBefore(startTime) || !nowTime.isAfter(endTime);
            }
        }

        // Lógica:
        // - Si están definidas fecha y hora: si estamos dentro de AMBAS => denegar
        // (false).
        // - Si sólo fecha está definida: si estamos dentro de la fecha => denegar.
        // - Si sólo hora está definida: si estamos dentro de la hora => denegar.
        // - Si nada definido: permitir.
        if (dateDefined && timeDefined) {
            return !(inDateRange && inTimeRange);
        } else if (dateDefined) {
            return !inDateRange;
        } else if (timeDefined) {
            return !inTimeRange;
        } else {
            return true;
        }
    }

    @Override
    public AccessRestriction getRestriction() {
        // Devuelve la entidad si existe, o null si no hay regla.
        return repo.findFirstByRoleName(ROLE_APPLICANT).orElse(null);
    }

    @Override
    public AccessRestriction saveOrUpdate(AccessRestriction restriction) {
        Optional<AccessRestriction> existing = repo.findFirstByRoleName(ROLE_APPLICANT);
        if (existing.isPresent()) {
            restriction.setId(existing.get().getId());
        }
        // Aquí podrías validar (startDate <= endDate) si lo deseas
        return repo.save(restriction);
    }

    // -----------------------------------------------------
    // DTO helpers: toDto ahora devuelve un DTO con campos null
    // en lugar de devolver directamente null cuando no hay entidad.
    // -----------------------------------------------------
    public static AccessRestrictionDTO toDto(AccessRestriction e) {
        AccessRestrictionDTO d = new AccessRestrictionDTO();
        if (e == null) {
            // Si no hay regla configurada, devolver DTO con campos null
            d.setId(null);
            d.setRoleName(ROLE_APPLICANT);
            d.setStartDate(null);
            d.setEndDate(null);
            d.setStartTime(null);
            d.setEndTime(null);
            d.setEnabled(false); // por defecto false (puedes cambiar esto)
            d.setDescription(null);
            return d;
        }
        d.setId(e.getId());
        d.setRoleName(e.getRoleName());
        d.setStartDate(e.getStartDate() != null ? e.getStartDate().toString() : null);
        d.setEndDate(e.getEndDate() != null ? e.getEndDate().toString() : null);
        d.setStartTime(e.getStartTime() != null ? e.getStartTime().toString() : null);
        d.setEndTime(e.getEndTime() != null ? e.getEndTime().toString() : null);
        d.setEnabled(e.isEnabled());
        d.setDescription(e.getDescription());
        return d;
    }

    public static AccessRestriction fromDto(AccessRestrictionDTO d) {
        AccessRestriction e = new AccessRestriction();
        e.setId(d.getId());
        e.setRoleName(d.getRoleName());
        // sólo parsear si vienen valores no-nulos
        if (d.getStartDate() != null && !d.getStartDate().isBlank()) {
            e.setStartDate(LocalDate.parse(d.getStartDate()));
        } else {
            e.setStartDate(null);
        }
        if (d.getEndDate() != null && !d.getEndDate().isBlank()) {
            e.setEndDate(LocalDate.parse(d.getEndDate()));
        } else {
            e.setEndDate(null);
        }
        if (d.getStartTime() != null && !d.getStartTime().isBlank()) {
            e.setStartTime(LocalTime.parse(d.getStartTime()));
        } else {
            e.setStartTime(null);
        }
        if (d.getEndTime() != null && !d.getEndTime().isBlank()) {
            e.setEndTime(LocalTime.parse(d.getEndTime()));
        } else {
            e.setEndTime(null);
        }
        e.setEnabled(d.isEnabled());
        e.setDescription(d.getDescription());
        return e;
    }
}
