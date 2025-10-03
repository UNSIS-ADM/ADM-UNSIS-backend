package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.VacancyDTO;
import com.unsis.admunsisbackend.model.Vacancy;
import com.unsis.admunsisbackend.repository.ApplicantRepository;
import com.unsis.admunsisbackend.repository.CareerChangeRequestRepository;
import com.unsis.admunsisbackend.repository.VacancyRepository;
import com.unsis.admunsisbackend.service.VacancyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class VacancyServiceImpl implements VacancyService {

    @Autowired
    private VacancyRepository vacancyRepo;

    @Autowired
    private ApplicantRepository applicantRepo;

    @Autowired
    private CareerChangeRequestRepository requestRepo;

    @Override
    public List<VacancyDTO> listVacancies(Integer year) {
        int y = (year != null ? year : Year.now().getValue());
        return vacancyRepo.findByAdmissionYear(y).stream().map(VacancyDTO::fromEntity).collect(Collectors.toList());
    }
    
    public VacancyServiceImpl(VacancyRepository vacancyRepo) {
        this.vacancyRepo = vacancyRepo;
    }

    // Normalizar minimal: trim (dejamos mayúsculas/acentos igual para respetar
    // texto original)
    private String normalizeCareer(String career) {
        return career == null ? null : career.trim();
    }

    @Override
    @Transactional
    public VacancyDTO upsertVacancy(String career, Integer year, Integer limitCount) {
        int y = (year != null ? year : Year.now().getValue());
        validateUpsertInputs(career, y, limitCount);

        var opt = vacancyRepo.findByCareerAndAdmissionYear(career, y);
        Vacancy v;
        int newCuposInserted = (limitCount != null ? limitCount : 0);

        if (opt.isPresent()) {
            v = opt.get();

            // primero recalculemos inscritos actuales desde applicants
            int inscritos = (int) applicantRepo.countByCareerAndAdmissionYear(career, y);
            v.setInscritosCount(inscritos);

            // Validación: no permitir cupos menores que los inscritos actuales
            if (newCuposInserted < inscritos) {
                throw new IllegalArgumentException(String.format(
                        "Los cupos (%d) no pueden ser menores que el número de inscritos ya cargados (%d).",
                        newCuposInserted, inscritos));
            }

            v.setCuposInserted(newCuposInserted);
            int available = Math.max(0, newCuposInserted - inscritos);
            v.setAvailableSlots(available);

            v = vacancyRepo.save(v);
        } else {
            v = new Vacancy();
            v.setCareer(career);
            v.setAdmissionYear(y);

            int inscritos = (int) applicantRepo.countByCareerAndAdmissionYear(career, y);
            v.setInscritosCount(inscritos);

            if (newCuposInserted < inscritos) {
                throw new IllegalArgumentException(String.format(
                        "Los cupos (%d) no pueden ser menores que el número de inscritos ya cargados (%d).",
                        newCuposInserted, inscritos));
            }

            v.setCuposInserted(newCuposInserted);
            v.setAvailableSlots(Math.max(0, newCuposInserted - inscritos));

            v = vacancyRepo.save(v);
        }

        return VacancyDTO.fromEntity(v);
    }

    /** Validaciones del input del upsert */
    private void validateUpsertInputs(String career, int year, Integer limitCount) {
        if (career == null || career.trim().isEmpty()) {
            throw new IllegalArgumentException("El parámetro 'career' es obligatorio.");
        }
        // Rango razonable para año: desde 2000 hasta (año actual + 50)
        int current = Year.now().getValue();
        if (year < 2000 || year > current + 50) {
            throw new IllegalArgumentException(String.format(
                    "El parámetro 'year' (%d) está fuera del rango permitido [2000, %d].", year, current + 50));
        }
        if (limitCount != null && limitCount < 0) {
            throw new IllegalArgumentException("El parámetro 'limitCount' no puede ser negativo.");
        }
    }

    @Override
    @Transactional
    public List<VacancyDTO> recalculateAll(Integer year) {
        int y = (year != null ? year : Year.now().getValue());
        List<Vacancy> vacs = vacancyRepo.findByAdmissionYear(y);
        for (Vacancy v : vacs) {
            actualizarContadores(v, y);
        }
        return vacs.stream().map(VacancyDTO::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VacancyDTO recalculateOne(String career, Integer year) {
        int y = (year != null ? year : Year.now().getValue());
        Vacancy v = vacancyRepo.findByCareerAndAdmissionYear(career, y)
                .orElseThrow(() -> new RuntimeException("Vacante no encontrada para " + career + " en " + y));
        actualizarContadores(v, y);
        return VacancyDTO.fromEntity(v);
    }

    private void actualizarContadores(Vacancy v, int year) {
        String career = v.getCareer();

        // inscritosCount se obtiene contándolos en applicants (lo que pides)
        long inscritos = applicantRepo.countByCareerAndAdmissionYear(career, year);
        v.setInscritosCount(safeLongToInt(inscritos));

        int cupos = Optional.ofNullable(v.getCuposInserted()).orElse(0);
        int available = Math.max(0, cupos - (int) inscritos);
        v.setAvailableSlots(available);

        vacancyRepo.save(v);
    }

    private int safeLongToInt(long value) {
        if (value > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        if (value < Integer.MIN_VALUE)
            return Integer.MIN_VALUE;
        return (int) value;
    }

     @Override
    @Transactional
    public Vacancy updateCuposInserted(String career, Integer admissionYear, Integer limit) {
        if (career == null || admissionYear == null || limit == null) {
            throw new IllegalArgumentException("career, admissionYear y limit son requeridos");
        }

        String normCareer = normalizeCareer(career);
        // Buscar vacancy (si no existe la creamos con inscritosCount = 0, pero NO lo recalculamos desde applicants)
        Vacancy v = vacancyRepo.findByCareerAndAdmissionYear(normCareer, admissionYear)
                .orElseGet(() -> {
                    Vacancy nv = new Vacancy();
                    nv.setCareer(normCareer);
                    nv.setAdmissionYear(admissionYear);
                    nv.setInscritosCount(0); // respetamos que Excel será la fuente (si luego subes Excel lo actualizará)
                    nv.setCuposInserted(0);
                    nv.setReservedCount(0);
                    nv.setAvailableSlots(0);
                    return nv;
                });

        boolean changed = false;

        // Actualizamos solo CUPOS (source of truth para inscritos sigue siendo Excel)
        if (!Optional.ofNullable(v.getCuposInserted()).orElse(0).equals(limit)) {
            v.setCuposInserted(limit);
            changed = true;
        }

        // Recalcular availableSlots con INSCRITOS que ya están guardados (no cambiamos inscritosCount)
        int inscritos = Optional.ofNullable(v.getInscritosCount()).orElse(0);
        int available = Math.max(0, limit - inscritos);
        if (!Optional.ofNullable(v.getAvailableSlots()).orElse(0).equals(available)) {
            v.setAvailableSlots(available);
            changed = true;
        }

        if (changed) {
            return vacancyRepo.save(v);
        } else {
            return v; // sin cambios
        }
    }

}
