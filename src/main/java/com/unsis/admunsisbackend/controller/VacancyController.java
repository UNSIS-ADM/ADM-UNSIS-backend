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

    @Autowired private VacancyService service;

    /** Listar todos los cupos para un año (por defecto, año actual) */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<VacancyDTO> list(@RequestParam(required=false) Integer year) {
        return service.listVacancies(year);
    }

    /** Crear o actualizar el cupo de una carrera para un año */
    @PutMapping("/{career}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public VacancyDTO upsert(
        @PathVariable String career,
        @RequestParam(required=false) Integer year,
        @RequestParam Integer limit
    ) {
        return service.upsertVacancy(career, year, limit);
    }
}
