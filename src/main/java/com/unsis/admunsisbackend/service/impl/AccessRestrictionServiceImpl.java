package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.AccessRestrictionDTO;
import com.unsis.admunsisbackend.model.AccessRestriction;
import com.unsis.admunsisbackend.repository.AccessRestrictionRepository;
import com.unsis.admunsisbackend.service.AccessRestrictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.time.LocalDate;

@Service
public class AccessRestrictionServiceImpl implements AccessRestrictionService {

    @Autowired
    private AccessRestrictionRepository repo;

    @Override
    public boolean isAccessAllowed(String roleName) {
        // Admin y User siempre tienen acceso
        if ("ROLE_ADMIN".equals(roleName) || "ROLE_USER".equals(roleName)) {
            return true;
        }

        if (!"ROLE_APPLICANT".equals(roleName)) {
            return true; // Otros roles sin restricción
        }

        Optional<AccessRestriction> opt = repo.findFirstByRoleName("ROLE_APPLICANT");
        if (opt.isEmpty()) {
            return true; // Si no hay regla configurada, acceso permitido
        }

        AccessRestriction rule = opt.get();

        if (!rule.isEnabled()) {
            return true; // Si está desactivada, acceso permitido
        }

        LocalDateTime now = LocalDateTime.now();

        // Validar fecha/hora
        boolean inDateRange = !now.toLocalDate().isBefore(rule.getStartDate())
                && !now.toLocalDate().isAfter(rule.getEndDate());

        boolean inTimeRange = !now.toLocalTime().isBefore(rule.getStartTime())
                && !now.toLocalTime().isAfter(rule.getEndTime());

        return !(inDateRange && inTimeRange); // Si está en el rango → acceso denegado
    }

    @Override
    public AccessRestriction getRestriction() {
        return repo.findFirstByRoleName("ROLE_APPLICANT").orElse(null);
    }

    @Override
    public AccessRestriction saveOrUpdate(AccessRestriction restriction) {
        Optional<AccessRestriction> existing = repo.findFirstByRoleName("ROLE_APPLICANT");
        if (existing.isPresent()) {
            restriction.setId(existing.get().getId());
        }
        return repo.save(restriction);
    }


    public static AccessRestrictionDTO toDto(AccessRestriction e) {
        if (e == null) return null;
        AccessRestrictionDTO d = new AccessRestrictionDTO();
        d.setId(e.getId());
        d.setRoleName(e.getRoleName());
        d.setStartDate(e.getStartDate().toString());
        d.setEndDate(e.getEndDate().toString());
        d.setStartTime(e.getStartTime().toString());
        d.setEndTime(e.getEndTime().toString());
        d.setEnabled(e.isEnabled());
        d.setDescription(e.getDescription());
        return d;
    }

    public static AccessRestriction fromDto(AccessRestrictionDTO d) {
        AccessRestriction e = new AccessRestriction();
        e.setId(d.getId());
        e.setRoleName(d.getRoleName());
        e.setStartDate(LocalDate.parse(d.getStartDate()));
        e.setEndDate(LocalDate.parse(d.getEndDate()));
        e.setStartTime(LocalTime.parse(d.getStartTime()));
        e.setEndTime(LocalTime.parse(d.getEndTime()));
        e.setEnabled(d.isEnabled());
        e.setDescription(d.getDescription());
        return e;
    }
}
