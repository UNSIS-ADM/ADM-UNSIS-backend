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
        Vacancy v = vacancyRepo.findByCareerAndAdmissionYear(career, y)
                .orElseGet(() -> {
                    Vacancy n = new Vacancy();
                    n.setCareer(career);
                    n.setAdmissionYear(y);
                    n.setLimitCount(limitCount != null ? limitCount : 0);
                    // availableSlots se recalculará abajo llamando a actualizarContadores
                    n.setAvailableSlots(limitCount != null ? limitCount : 0);
                    return n;
                });

                if (limitCount != null) {
            // Sumar el nuevo limitCount al actual (no sobrescribir)
            int nuevaLimit = safeAddInts(v.getLimitCount(), limitCount);
            v.setLimitCount(nuevaLimit);
            // Recalcular contadores y availableSlots con base en el nuevo limitCount
            actualizarContadores(v, y);
        } else {
            // Si no se cambia limitCount y availableSlots es null, inicializarlo
            if (v.getAvailableSlots() == null) {
                v.setAvailableSlots(v.getLimitCount());
            }
            // Guardar por si es nueva
            v = vacancyRepo.save(v);
        }
        Vacancy saved = vacancyRepo.save(v);
        return VacancyDTO.fromEntity(saved);
    }
             
/* 
        v.setLimitCount(limitCount != null ? limitCount : v.getLimitCount());
        // If creating new or limit changed, we may want to adjust availableSlots — keep
        // simple:
        if (v.getAvailableSlots() == null)
            v.setAvailableSlots(v.getLimitCount());
        Vacancy saved = vacancyRepo.save(v);
        return VacancyDTO.fromEntity(saved);
    }*/

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
        /* 
        v.setAvailableSlots(Math.max(v.getLimitCount() - (int) accepted - (int) pending, 0));
        vacancyRepo.save(v);*/
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
