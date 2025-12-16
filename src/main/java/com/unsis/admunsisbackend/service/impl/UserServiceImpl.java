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
import com.unsis.admunsisbackend.dto.AdminUserUpdateDTO;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Override
    @Transactional
    public UserResponseDTO adminCreateOrUpdateUser(AdminUserUpdateDTO dto) {
        if (dto == null) throw new IllegalArgumentException("DTO es null");

        User user;
        boolean creating = (dto.getId() == null);

        if (creating) {
            // crear nuevo usuario: validar username no exista
            if (dto.getUsername() == null || dto.getUsername().isBlank()) {
                throw new RuntimeException("username requerido para crear usuario");
            }
            if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
                throw new RuntimeException("Ya existe un usuario con username=" + dto.getUsername());
            }
            user = new User();
        } else {
            user = userRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id=" + dto.getId()));
        }

        // VALIDACIÓN de cambio de username (si viene y es distinto)
        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            if (!dto.getUsername().equals(user.getUsername())) {
                // verificar que no exista otro con ese username
                Optional<User> byUsername = userRepository.findByUsername(dto.getUsername());
                if (byUsername.isPresent() && !byUsername.get().getId().equals(user.getId())) {
                    throw new RuntimeException("El username " + dto.getUsername() + " ya está en uso por otro usuario.");
                }
                user.setUsername(dto.getUsername());
            }
        }

        // fullName (si viene)
        if (dto.getFullName() != null) {
            user.setFullName(dto.getFullName());
        }

        // password: si viene y no está vacío => encriptar y actualizar
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        // active: si viene (habilitar/deshabilitar)
        if (dto.getActive() != null) {
            // evitar que un admin se desactive a sí mismo (precaución)
            String currentUsername = SecurityContextHolder.getContext().getAuthentication() != null
                    ? SecurityContextHolder.getContext().getAuthentication().getName()
                    : null;
            if (currentUsername != null && currentUsername.equals(user.getUsername()) && !dto.getActive()) {
                throw new RuntimeException("No puede desactivar su propia cuenta de administrador.");
            }
            user.setActive(dto.getActive());
        }

        // roles: si vienen, mapear a entidades Role
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            Set<Role> roles = dto.getRoles().stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + roleName)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        // Si era creación, asegurarnos campos mínimos
        if (creating) {
            // si no mandaron password, le ponemos un password temporal (recomendable: requerir password)
            if (user.getPassword() == null) {
                user.setPassword(passwordEncoder.encode("changeme"));
            }
            if (user.getFullName() == null) user.setFullName(user.getUsername());
            if (user.getActive() == null) user.setActive(true);
            // si no mandaron roles, asignar ROLE_USER por defecto
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                Role defaultRole = roleRepository.findByName("ROLE_USER")
                        .orElseThrow(() -> new RuntimeException("Rol ROLE_USER no existe"));
                user.setRoles(Set.of(defaultRole));
            }
        }

        User saved = userRepository.save(user);
        return convertToDTO(saved);
    }

}
