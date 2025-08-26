package com.unsis.admunsisbackend.controller;


import com.unsis.admunsisbackend.dto.VacancyDTO;
import com.unsis.admunsisbackend.service.VacancyService;
import org.springframework.beans.factory.annotation.Autowired;
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

    /** Crear o actualizar cupo de una carrera para un año */
    @PutMapping("/{career}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public VacancyDTO upsert(
            @PathVariable String career,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer limitCount) {
        return vacancyService.upsertVacancy(career, year, limitCount);
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
