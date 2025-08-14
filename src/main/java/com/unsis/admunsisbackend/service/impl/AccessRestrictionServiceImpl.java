package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.model.AccessRestriction;
import com.unsis.admunsisbackend.repository.AccessRestrictionRepository;
import com.unsis.admunsisbackend.service.AccessRestrictionService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class AccessRestrictionServiceImpl implements AccessRestrictionService {

    @Autowired
    private AccessRestrictionRepository repo;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId ZONE = ZoneId.of("America/Mexico_City"); // cambiar si necesitas otra zona

    @Override
    public boolean isAccessAllowed(String roleName) {
        List<AccessRestriction> rules = repo.findByRoleNameAndEnabledTrue(roleName);
        if (rules == null || rules.isEmpty())
            return true; // sin reglas = permitido

        ZonedDateTime now = ZonedDateTime.now(); // o usar zona configurable
        for (AccessRestriction r : rules) {
            if (isNowInWindow(now, r)) {
                // Si cae dentro de alguna regla de restricción → no permitido
                return false;
            }
        }
        return true;
    }

    private boolean isNowInWindow(ZonedDateTime now, AccessRestriction r) {
        // minuto de inicio de la semana (MONDAY 00:00)
        int minutesPerWeek = 7 * 24 * 60;
        LocalTime s = LocalTime.parse(r.getStartTime(), TIME_FMT);
        LocalTime e = LocalTime.parse(r.getEndTime(), TIME_FMT);
        int startOffset = (r.getStartDay() - 1) * 24 * 60 + s.getHour() * 60 + s.getMinute();
        int endOffset = (r.getEndDay() - 1) * 24 * 60 + e.getHour() * 60 + e.getMinute();
        if (endOffset <= startOffset)
            endOffset += minutesPerWeek; // cruza semana

        DayOfWeek dow = now.getDayOfWeek();
        int nowOffset = (dow.getValue() - 1) * 24 * 60 + now.getHour() * 60 + now.getMinute();
        // si nowOffset < startOffset puede que la ventana sea la que empezó la semana
        // pasada (wrap)
        if (nowOffset < startOffset)
            nowOffset += minutesPerWeek;
        return nowOffset >= startOffset && nowOffset < endOffset;
    }

    @Override
    public List<AccessRestriction> listForRole(String roleName) {
        return repo.findByRoleNameAndEnabledTrue(roleName);
    }

    @Override
    public AccessRestriction create(AccessRestriction w) {
        return repo.save(w);
    }

    @Override
    public AccessRestriction update(Long id, AccessRestriction w) {
        AccessRestriction existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Restricción no encontrada"));
        existing.setRoleName(w.getRoleName());
        existing.setStartDay(w.getStartDay());
        existing.setStartTime(w.getStartTime());
        existing.setEndDay(w.getEndDay());
        existing.setEndTime(w.getEndTime());
        existing.setEnabled(w.isEnabled());
        existing.setDescription(w.getDescription());
        return repo.save(existing);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }
}