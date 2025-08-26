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
            // Vacante existente: aplicamos comportamiento de SUMA al limitCount
            v = opt.get();

            if (limitCount != null) {
                // Contadores actuales para validar que no se reduzca el límite por debajo de lo
                // ya comprometido
                long accepted = applicantRepo.countByCareerAndAdmissionYearAndStatus(career, y, "ACEPTADO");
                long pending = requestRepo.countPendingByNewCareerAndYear(career, y);

                int currentLimit = v.getLimitCount() != null ? v.getLimitCount() : 0;
                int nuevaLimit = safeAddInts(currentLimit, limitCount); // suma segura

                // Validación: no permitir que el nuevo límite sea menor que
                // aceptados+pendientes
                if ((long) nuevaLimit < accepted + pending) {
                    throw new IllegalArgumentException(String.format(
                            "El nuevo limitCount (%d) no puede ser menor que la suma de aceptados y pendientes (%d).",
                            nuevaLimit, accepted + pending));
                }

                v.setLimitCount(nuevaLimit);
                // Recalcular accepted/pending/available y persistir desde aquí
                actualizarContadores(v, y);
            } else {
                // No se cambia limitCount; solo asegurar availableSlots inicializado si es null
                if (v.getAvailableSlots() == null) {
                    v.setAvailableSlots(v.getLimitCount() != null ? v.getLimitCount() : 0);
                    vacancyRepo.save(v);
                }
            }

        } else {
            // Vacante nueva: asignar (NO sumar) el limitCount entrante
            v = new Vacancy();
            v.setCareer(career);
            v.setAdmissionYear(y);

            int initialLimit = limitCount != null ? limitCount : 0;

            // Validación extra: no permitir negative (ya validado arriba, pero por
            // seguridad aquí también)
            if (initialLimit < 0) {
                throw new IllegalArgumentException("limitCount no puede ser negativo.");
            }

            v.setLimitCount(initialLimit);
            // En vacante nueva accepted/pending = 0, así que available = initialLimit
            v.setAcceptedCount(0);
            v.setPendingCount(0);
            v.setAvailableSlots(initialLimit);

            // Guardar la nueva vacante
            v = vacancyRepo.save(v);
        }

        // Nota: actualizarContadores ya hace un save() internamente, así que aquí
        // devolvemos el estado actual de 'v' (ya persistido).
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

        // Recalcular availableSlots siempre en base al limitCount y los contadores actuales
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

/*
 * Suma dos ints de forma segura evitando overflow; si overflow, devuelve Integer.MAX_VALUE o MIN_VALUE.
 */

    private int safeAddInts(Integer a, Integer b) {
        int va = a != null ? a : 0;
        int vb = b != null ? b : 0;
        long sum = (long) va + (long) vb;
        if (sum > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (sum < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) sum;
    }
}
