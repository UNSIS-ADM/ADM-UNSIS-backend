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
        return vacancyRepo.findByAdmissionYear(y)
                .stream()
                .map(VacancyDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VacancyDTO upsertVacancy(String career, Integer year, Integer limitCount) {
        int y = (year != null ? year : Year.now().getValue());

        // Validaciones básicas de entrada
        validateUpsertInputs(career, y, limitCount);

        var opt = vacancyRepo.findByCareerAndAdmissionYear(career, y);
        Vacancy v;

        if (opt.isPresent()) {
            // Vacante existente: ahora REEMPLAZA el limitCount
            v = opt.get();

            if (limitCount != null) {
                // Contadores actuales
                long accepted = applicantRepo.countByCareerAndAdmissionYearAndStatus(career, y, "ACEPTADO");
                long rejected = applicantRepo.countByCareerAndAdmissionYearAndStatus(career, y, "RECHAZADO");
                long pending = requestRepo.countPendingByNewCareerAndYear(career, y);

                // total decisions = accepted + rejected
                long totalDecisions = accepted + rejected;

                // Validar que el nuevo límite no sea menor que totalDecisions + pending
                if ((long) limitCount < totalDecisions + pending) {
                    throw new IllegalArgumentException(String.format(
                            "El nuevo limitCount (%d) no puede ser menor que la suma de decisiones (aceptados+rechazados=%d) más pendientes (%d).",
                            limitCount, totalDecisions, pending));
                }

                // Actualizamos el limitCount
                v.setLimitCount(limitCount);
                v.setAcceptedCount(safeLongToInt(accepted));
                v.setRejectedCount(safeLongToInt(rejected));
                v.setPendingCount(safeLongToInt(pending));
                v.setTotalCount(safeLongToInt(totalDecisions));

                // Recalcular availableSlots en base a totalDecisions + pending
                int available = (int) Math.max(0, (long) limitCount - totalDecisions - pending);
                v.setAvailableSlots(available);
                v = vacancyRepo.save(v);
            }

        } else {
            // Vacante nueva: asignar directamente el limitCount entrante
            v = new Vacancy();
            v.setCareer(career);
            v.setAdmissionYear(y);

            int initialLimit = limitCount != null ? limitCount : 0;
            if (initialLimit < 0) {
                throw new IllegalArgumentException("limitCount no puede ser negativo.");
            }

            v.setLimitCount(initialLimit);
            v.setAcceptedCount(0);
            v.setRejectedCount(0); // <-- agregado
            v.setPendingCount(0);
            v.setTotalCount(0); // <-- agregado
            v.setAvailableSlots(initialLimit);

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

        long accepted = applicantRepo.countByCareerAndAdmissionYearAndStatus(career, year, "ACEPTADO");
        long rejected = applicantRepo.countByCareerAndAdmissionYearAndStatus(career, year, "RECHAZADO");
        long pending = requestRepo.countPendingByNewCareerAndYear(career, year);

        long totalDecisions = accepted + rejected;

        v.setAcceptedCount(safeLongToInt(accepted));
        v.setRejectedCount(safeLongToInt(rejected));
        v.setPendingCount(safeLongToInt(pending));
        v.setTotalCount(safeLongToInt(totalDecisions));

        int limit = v.getLimitCount() != null ? v.getLimitCount() : 0;
        long availableCalc = (long) limit - totalDecisions - pending;
        int available = availableCalc <= 0 ? 0 : safeLongToInt(availableCalc);

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
}
