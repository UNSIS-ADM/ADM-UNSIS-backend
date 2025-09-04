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
                long pending = requestRepo.countPendingByNewCareerAndYear(career, y);

                // Validar que el nuevo límite no sea menor que aceptados+pendientes
                if ((long) limitCount < accepted + pending) {
                    throw new IllegalArgumentException(String.format(
                            "El nuevo limitCount (%d) no puede ser menor que la suma de aceptados y pendientes (%d).",
                            limitCount, accepted + pending));
                }
                // Calculamos diferencia entre nuevo límite y límite actual (oldLimit)
                int oldLimit = v.getLimitCount() != null ? v.getLimitCount() : 0;
                int delta = limitCount - oldLimit;

                // Actualizamos el limitCount
                v.setLimitCount(limitCount);

                // Según tu requerimiento: available_slots será la diferencia positiva entre
                // nuevo y viejo límite
                // (si delta < 0 dejamos 0)
                int newAvailable = Math.max(0, delta);
                v.setAvailableSlots(newAvailable);

                // Recalcular contadores con el nuevo límite
                //actualizarContadores(v, y);

                // NO llamamos a actualizarContadores ni tocamos acceptedCount/pendingCount aquí
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
            v.setPendingCount(0);
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
        // Rango razonable para año: desde 2000 hasta (año actual + 5)
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
        long pending = requestRepo.countPendingByNewCareerAndYear(career, year);

        v.setAcceptedCount((int) accepted);
        v.setPendingCount((int) pending);

        // Recalcular availableSlots siempre en base al limitCount y los contadores
        // actuales
        int limit = v.getLimitCount() != null ? v.getLimitCount() : 0;
        long availableCalc = (long) limit - accepted - pending;
        int available = availableCalc <= 0 ? 0 : safeLongToInt(availableCalc);

        v.setAvailableSlots(available);
        vacancyRepo.save(v);
    }

    /**
     * Convierte un long a int de forma segura: si el long está fuera del rango int,
     * lo fuerza a Integer.MAX_VALUE o Integer.MIN_VALUE según corresponda.
     */
    private int safeLongToInt(long value) {
        if (value > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        if (value < Integer.MIN_VALUE)
            return Integer.MIN_VALUE;
        return (int) value;
    }
}
