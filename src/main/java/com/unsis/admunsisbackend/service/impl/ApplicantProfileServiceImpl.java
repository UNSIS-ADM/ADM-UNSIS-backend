package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.ApplicantProfileDTO;
import com.unsis.admunsisbackend.model.Applicant;
import com.unsis.admunsisbackend.model.AdmissionResult;
import com.unsis.admunsisbackend.repository.ApplicantRepository;
import com.unsis.admunsisbackend.repository.AdmissionResultRepository;
import com.unsis.admunsisbackend.service.ApplicantProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApplicantProfileServiceImpl implements ApplicantProfileService {

    @Autowired private ApplicantRepository applicantRepo;
    @Autowired private AdmissionResultRepository resultRepo;

    @Override
    public ApplicantProfileDTO getMyProfile(String username) {
        // 1) Cargar el Applicant asociado al username
        Applicant applicant = applicantRepo.findByUser_Username(username)
            .orElseThrow(() -> new RuntimeException("Aspirante no encontrado"));
        
        // 2) Buscar su último resultado (si existe)
        AdmissionResult last = resultRepo
            .findTopByApplicantOrderByCreatedAtDesc(applicant)
            .orElse(null);

        // 3) Mapear al DTO
        ApplicantProfileDTO dto = new ApplicantProfileDTO();
        dto.setFullName(applicant.getUser().getFullName());
        dto.setFicha(applicant.getFicha());
        dto.setCareer(applicant.getCareer());
        if (last != null) {
            dto.setStatus(last.getStatus());
            dto.setScore(last.getScore());
            dto.setResultDate(last.getCreatedAt());
        }
        return dto;
    }
}
