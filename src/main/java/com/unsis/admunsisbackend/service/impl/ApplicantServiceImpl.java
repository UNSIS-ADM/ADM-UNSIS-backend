package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.ApplicantResponseDTO;
import com.unsis.admunsisbackend.model.Applicant;
import com.unsis.admunsisbackend.repository.ApplicantRepository;
import com.unsis.admunsisbackend.service.ApplicantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApplicantServiceImpl implements ApplicantService {
    @Autowired
    private ApplicantRepository repo;

    @Override
    public List<ApplicantResponseDTO> getAllApplicants() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
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
}
