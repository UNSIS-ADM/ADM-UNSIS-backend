package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.UserResponseDTO;
import com.unsis.admunsisbackend.dto.ApplicantResponseDTO;
import com.unsis.admunsisbackend.model.User;
import com.unsis.admunsisbackend.model.Applicant;
import com.unsis.admunsisbackend.repository.UserRepository;
import com.unsis.admunsisbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public ApplicantResponseDTO getApplicantProfile(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getApplicant() == null) {
            throw new AccessDeniedException("El usuario no es un aspirante");
        }

        return convertToApplicantDTO(user, user.getApplicant());
    }

    private UserResponseDTO convertToDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setActive(user.getActive());
        dto.setRoles(user.getRoles().stream()
            .map(role -> role.getName())
            .collect(Collectors.toSet()));
        return dto;
    }

    private ApplicantResponseDTO convertToApplicantDTO(User user, Applicant applicant) {
        ApplicantResponseDTO dto = new ApplicantResponseDTO();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setCurp(applicant.getCurp());
        dto.setExamRoom(applicant.getExamRoom());
        dto.setExamDate(applicant.getExamDate());
        dto.setStatus(applicant.getStatus());
        return dto;
    }
}