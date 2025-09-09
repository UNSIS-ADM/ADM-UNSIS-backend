package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.UserResponseDTO;
import com.unsis.admunsisbackend.dto.ApplicantResponseDTO;
import com.unsis.admunsisbackend.model.User;
import com.unsis.admunsisbackend.model.Applicant;
import com.unsis.admunsisbackend.model.Role;
import com.unsis.admunsisbackend.repository.ApplicantRepository;
import com.unsis.admunsisbackend.repository.RoleRepository;
import com.unsis.admunsisbackend.repository.UserRepository;
import com.unsis.admunsisbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.Optional;
import java.util.List;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicantRepository applicantRepository;

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

    @Override
    @Transactional
    public void syncUserCredentialsForApplicant(Applicant applicant) {
        if (applicant == null) {
            throw new IllegalArgumentException("Applicant es null");
        }

        String fichaStr = String.valueOf(applicant.getFicha()); // username = ficha (ajusta si quieres otro)
        String curp = applicant.getCurp() == null ? "" : applicant.getCurp().trim();

        User user = applicant.getUser();

        if (user == null) {
            // crear nuevo usuario
            Optional<User> existing = userRepository.findByUsername(fichaStr);
            if (existing.isPresent()) {
                throw new RuntimeException(
                        "Ya existe un usuario con username=" + fichaStr + ". No se puede crear automáticamente.");
            }

            User newUser = new User();
            newUser.setUsername(fichaStr);
            newUser.setPassword(passwordEncoder.encode(curp));

            // <-- FIX: obtener fullName de fuentes disponibles (user asociado o fallback)
            String fullName = "";
            if (applicant.getUser() != null && applicant.getUser().getFullName() != null) {
                fullName = applicant.getUser().getFullName();
            } else if (applicant.getCurp() != null) {
                // fallback razonable: usar CURP si no hay nombre
                fullName = applicant.getCurp();
            }
            newUser.setFullName(fullName);
            // <-- end fix

            newUser.setActive(true);

            Role role = roleRepository.findByName("ROLE_APPLICANT")
                    .orElseGet(() -> roleRepository.findByName("ROLE_USER")
                            .orElseThrow(() -> new RuntimeException("Rol ROLE_APPLICANT o ROLE_USER no existe")));
            newUser.setRoles(Set.of(role));

            userRepository.save(newUser);

            applicant.setUser(newUser);
            applicantRepository.save(applicant);
            return;
        }

        // si ya existe usuario vinculado -> actualizar username y/o password si cambian
        boolean changed = false;

        if (!fichaStr.equals(user.getUsername())) {
            Optional<User> byNewUsername = userRepository.findByUsername(fichaStr);
            if (byNewUsername.isPresent() && !byNewUsername.get().getId().equals(user.getId())) {
                throw new RuntimeException(
                        "No se puede cambiar username a " + fichaStr + " porque ya está en uso por otro usuario.");
            }
            user.setUsername(fichaStr);
            changed = true;
        }

        if (curp != null && !curp.isEmpty()) {
            if (!passwordEncoder.matches(curp, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(curp));
                changed = true;
            }
        }

        if (changed) {
            userRepository.save(user);
        }
    }
}
