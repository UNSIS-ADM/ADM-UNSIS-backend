package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.VacancyDTO;
import com.unsis.admunsisbackend.model.Vacancy;
import com.unsis.admunsisbackend.service.VacancyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vacancies")
public class VacancyController {

    @Autowired
    private VacancyService vacancyService;

    /** Listar cupos por año (por defecto año actual) */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public List<VacancyDTO> list(@RequestParam(required = false) Integer year) {
        return vacancyService.listVacancies(year);
    }


      // PUT /api/admin/vacancies/{career}?year=2025&limit=10
    @PutMapping("/{career}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateCupos(
            @PathVariable("career") String career,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer limit) {

        try {
            Vacancy v = vacancyService.updateCuposInserted(career, year, limit);
            return ResponseEntity.ok(v);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            // Log el error real en logger
            return ResponseEntity.status(500).body("Error al actualizar cupos: " + ex.getMessage());
        }
    }


    /** Recalcula todos los contadores (accepted/pending/available) */
    @PutMapping("/recalculate")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public List<VacancyDTO> recalcAll(@RequestParam(required = false) Integer year) {
        return vacancyService.recalculateAll(year);
    }

    /** Recalcula una carrera concreta */
    @PutMapping("/recalculate/{career}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public VacancyDTO recalcOne(@PathVariable String career,
            @RequestParam(required = false) Integer year) {
        return vacancyService.recalculateOne(career, year);
    }
}
