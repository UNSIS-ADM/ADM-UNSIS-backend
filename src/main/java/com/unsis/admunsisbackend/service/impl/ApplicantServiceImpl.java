package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.ApplicantResponseDTO;
import com.unsis.admunsisbackend.model.Applicant;
import com.unsis.admunsisbackend.model.Vacancy;
import com.unsis.admunsisbackend.repository.ApplicantRepository;
import com.unsis.admunsisbackend.repository.VacancyRepository;
import com.unsis.admunsisbackend.service.ApplicantService;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApplicantServiceImpl implements ApplicantService {

    @Autowired
    private ApplicantRepository applicantRepository;
    
    @Autowired
    private ApplicantRepository applicantRepo;
    
    @Autowired private VacancyRepository vacancyRepo;

    @Override
    public List<ApplicantResponseDTO> getAllApplicants() {
        List<Applicant> applicants = applicantRepository.findAll();

        return applicants.stream().map(applicant -> {
            ApplicantResponseDTO dto = new ApplicantResponseDTO();
            dto.setId(applicant.getId());
            dto.setCurp(applicant.getCurp());
            dto.setFullName(applicant.getUser().getFullName());
            dto.setCareer(applicant.getCareer());
            dto.setLocation(applicant.getLocation());
            dto.setPhone(applicant.getPhone());
            dto.setExamRoom(applicant.getExamRoom());
            dto.setExamDate(applicant.getExamDate());
            dto.setStatus(applicant.getStatus());
            dto.setLastLogin(applicant.getUser().getLastLogin());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
  @Transactional
  public void changeCareerByCurp(String curp, String newCareer) {
      // 1) Localiza al aspirante por CURP
    Applicant a = applicantRepo.findByCurp(curp)
        .orElseThrow(() -> new RuntimeException("No existe aspirante con CURP: " + curp));
    int year = a.getAdmissionYear();

    // 2) Validaciones (por ejemplo, no medicina, cupo disponible, no misma carrera)
    if ("LICENCIATURA EN MEDICINA".equalsIgnoreCase(newCareer)) {
      throw new RuntimeException("No puedes cambiarte a Medicina");
    }
    // 2) Validaciones (por ejemplo, no medicina, cupo disponible, no misma carrera)
    if (a.getCareer().equalsIgnoreCase(newCareer)) {
      throw new RuntimeException("Ya estás inscrito en esa carrera");
    }

    // validar cupos
    long inscritos = applicantRepo.countByCareerAndAdmissionYear(newCareer, year);
    Vacancy vac = vacancyRepo.findByCareerAndAdmissionYear(newCareer, year)
        .orElseThrow(() -> new RuntimeException("Vacantes no configuradas para " + newCareer + " en " + year));
    if (inscritos >= vac.getLimitCount()) {
      throw new RuntimeException("Cupo agotado para " + newCareer);
    }

    // todo OK → actualizar
    a.setCareer(newCareer);
    applicantRepo.save(a);
  }
}
