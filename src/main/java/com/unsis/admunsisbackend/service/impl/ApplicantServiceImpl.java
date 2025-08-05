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

    private ApplicantRepository repo;

    @Override
    public List<ApplicantResponseDTO> getAllApplicants() {
<<<<<<< Updated upstream
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
=======
        List<Applicant> applicants = applicantRepository.findAll();

        return applicants.stream().map(applicant -> {
            ApplicantResponseDTO dto = new ApplicantResponseDTO();
            dto.setId(applicant.getId());
            dto.setFicha(applicant.getFicha()); // ← aquí
            dto.setCurp(applicant.getCurp());
            dto.setFullName(applicant.getUser().getFullName());
            dto.setCareer(applicant.getCareer());
            dto.setLocation(applicant.getLocation());
            dto.setPhone(applicant.getPhone());
            dto.setExamRoom(applicant.getExamRoom());
            dto.setExamDate(applicant.getExamDate());
            dto.setStatus(applicant.getStatus());
            return dto;
        }).collect(Collectors.toList());
>>>>>>> Stashed changes
    }

    @Override
    public List<ApplicantResponseDTO> searchApplicants(
            Long ficha,
            String curp,
            String career,
            String fullName) {

        List<Applicant> results;

        if (ficha != null) {
            results = repo.findByFicha(ficha).map(List::of).orElse(List.of());
        } else if (curp != null && !curp.isBlank()) {
            results = repo.findByCurpContainingIgnoreCase(curp);
        } else if (career != null && !career.isBlank()) {
            results = repo.findByCareerContainingIgnoreCase(career);
        } else if (fullName != null && !fullName.isBlank()) {
            results = repo.findByUser_FullNameContainingIgnoreCase(fullName);
        } else {
            results = repo.findAll();
        }

        return results.stream().map(this::toDto).collect(Collectors.toList());
    }

    private ApplicantResponseDTO toDto(Applicant a) {
        ApplicantResponseDTO dto = new ApplicantResponseDTO();
        dto.setId(a.getId());
        dto.setFicha(a.getFicha());
        dto.setCurp(a.getCurp());
        dto.setFullName(a.getUser().getFullName());
        dto.setCareer(a.getCareer());
        dto.setLocation(a.getLocation());
        dto.setPhone(a.getPhone());
        dto.setExamRoom(a.getExamRoom());
        dto.setExamDate(a.getExamDate());
        dto.setStatus(a.getStatus());
        dto.setLastLogin(a.getUser().getLastLogin());
        return dto;
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
