package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.dto.VacancyDTO;
import java.util.List;

public interface VacancyService {
    List<VacancyDTO> listVacancies(Integer year);
    VacancyDTO upsertVacancy(String career, Integer year, Integer limitCount);
    List<VacancyDTO> recalculateAll(Integer year);
    VacancyDTO recalculateOne(String career, Integer year);
}
