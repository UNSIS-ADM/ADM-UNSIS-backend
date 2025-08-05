package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.VacancyDTO;
import com.unsis.admunsisbackend.model.Vacancy;
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

    @Autowired private VacancyRepository vacRepo;

    @Override
    public List<VacancyDTO> listVacancies(Integer year) {
        int y = (year != null ? year : Year.now().getValue());
        return vacRepo.findByAdmissionYear(y)
                    .stream().map(VacancyDTO::fromEntity)
                    .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VacancyDTO upsertVacancy(String career, Integer year, Integer limitCount) {
        int y = (year != null ? year : Year.now().getValue());
        // Buscamos una vacante existente o creamos una nueva
        Vacancy v = vacRepo.findByCareerAndAdmissionYear(career, y)
            .orElseGet(() -> {
                Vacancy n = new Vacancy();
                n.setCareer(career);
                n.setAdmissionYear(y);
                return n;
            });
        v.setLimitCount(limitCount);
        Vacancy saved = vacRepo.save(v);
        return VacancyDTO.fromEntity(saved);
    }
}
