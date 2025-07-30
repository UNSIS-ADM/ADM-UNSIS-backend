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
    private ApplicantRepository applicantRepository;

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
}
