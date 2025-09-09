package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.ApplicantAdminUpdateDTO;
import com.unsis.admunsisbackend.dto.ApplicantResponseDTO;
import com.unsis.admunsisbackend.model.AdmissionResult;
import com.unsis.admunsisbackend.model.Applicant;
import com.unsis.admunsisbackend.model.User;
import com.unsis.admunsisbackend.model.Vacancy;
import com.unsis.admunsisbackend.repository.AdmissionResultRepository;
import com.unsis.admunsisbackend.repository.ApplicantRepository;
import com.unsis.admunsisbackend.repository.VacancyRepository;
import com.unsis.admunsisbackend.service.ApplicantService;
import com.unsis.admunsisbackend.service.UserService;
import com.unsis.admunsisbackend.repository.UserRepository;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// dentro de la clase ApplicantServiceImpl, junto a applicantRepo y vacancyRepo

@Service
public class ApplicantServiceImpl implements ApplicantService {
    private static final Logger logger = LoggerFactory.getLogger(ApplicantServiceImpl.class);

    @Autowired
    private ApplicantRepository applicantRepo;

    @Autowired
    private VacancyRepository vacancyRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AdmissionResultRepository admissionResultRepo;

    @Autowired
    private UserService userService;

    @Override
    @Transactional // read-only impl via jakarta.transaction.Transactional (for lazy fetch safety)
    public List<ApplicantResponseDTO> getAllApplicants(Integer year) {
        List<Applicant> list;
        if (year != null) {
            list = applicantRepo.findByAdmissionYear(year);
        } else {
            list = applicantRepo.findAll();
        }
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ApplicantResponseDTO> searchApplicants(
            Long ficha,
            String curp,
            String career,
            String fullName) {

        List<Applicant> results;

        if (ficha != null) {
            results = applicantRepo.findByFicha(ficha).map(List::of).orElse(List.of());
        } else if (curp != null && !curp.isBlank()) {
            results = applicantRepo.findByCurpContainingIgnoreCase(curp);
        } else if (career != null && !career.isBlank()) {
            results = applicantRepo.findByCareerContainingIgnoreCase(career);
        } else if (fullName != null && !fullName.isBlank()) {
            results = applicantRepo.findByUser_FullNameContainingIgnoreCase(fullName);
        } else {
            results = applicantRepo.findAll();
        }

        return results.stream().map(this::toDto).collect(Collectors.toList());
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

    @Override
    public ApplicantResponseDTO getApplicantById(Long id) {
        Applicant a = applicantRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aspirante no encontrado"));

        ApplicantResponseDTO dto = toDto(a);
        // obtener último admission result y sobrescribir campos en el DTO
        admissionResultRepo.findTopByApplicantOrderByCreatedAtDesc(a).ifPresent(res -> {
            dto.setCareerAtResult(res.getCareerAtResult());
            dto.setScore(res.getScore()); // necesitarías añadir score en ApplicantResponseDTO si quieres mostrarlo
            dto.setResultDate(res.getCreatedAt()); // añade campo resultDate en DTO si hace falta
        });

        return dto;
    }

    @Override
    @Transactional
    public ApplicantResponseDTO updateApplicantByAdmin(Long id, ApplicantAdminUpdateDTO dto, String adminUsername) {
        Applicant a = applicantRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aspirante no encontrado"));

        // FICHA (unicidad por año
        /*
         * if (dto.getFicha() != null) {
         * Long nuevaFicha = dto.getFicha();
         * if (!nuevaFicha.equals(a.getFicha()) &&
         * applicantRepo.existsByFicha(nuevaFicha)) {
         * throw new ResponseStatusException(HttpStatus.CONFLICT,
         * "Ya existe un aspirante con ficha: " + nuevaFicha);
         * }
         * a.setFicha(nuevaFicha);
         * }
         * 
         * // CURP (unicidad)
         * if (dto.getCurp() != null) {
         * String nuevaCurp = dto.getCurp().trim().toUpperCase();
         * if (!nuevaCurp.equalsIgnoreCase(a.getCurp()) &&
         * applicantRepo.existsByCurp(nuevaCurp)) {
         * throw new ResponseStatusException(HttpStatus.CONFLICT,
         * "Ya existe un aspirante con CURP: " + nuevaCurp);
         * }
         * a.setCurp(nuevaCurp);
         * }
         */

        if (dto.getFicha() != null) {
            Long nuevaFicha = dto.getFicha();
            if (!nuevaFicha.equals(a.getFicha()) &&
                    applicantRepo.existsByFichaAndAdmissionYear(nuevaFicha, a.getAdmissionYear())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya existe un aspirante con ficha: " + nuevaFicha + " en el año " + a.getAdmissionYear());
            }
            a.setFicha(nuevaFicha);
        }

        // CURP (unicidad)
        if (dto.getCurp() != null) {
            String nuevaCurp = dto.getCurp().trim().toUpperCase();
            if (!nuevaCurp.equalsIgnoreCase(a.getCurp()) &&
                    applicantRepo.existsByCurpAndAdmissionYear(nuevaCurp, a.getAdmissionYear())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya existe un aspirante con CURP: " + nuevaCurp + " en el año " + a.getAdmissionYear());
            }
            a.setCurp(nuevaCurp);
        }

        // Otros campos Applicant
        if (dto.getCareer() != null)
            a.setCareer(dto.getCareer());
        if (dto.getLocation() != null)
            a.setLocation(dto.getLocation());
        if (dto.getExamRoom() != null)
            a.setExamRoom(dto.getExamRoom());
        if (dto.getExamAssigned() != null)
            a.setExamAssigned(dto.getExamAssigned());
        if (dto.getExamDate() != null)
            a.setExamDate(dto.getExamDate());
        if (dto.getAdmissionYear() != null)
            a.setAdmissionYear(dto.getAdmissionYear());
            
        // Actualizar User relacionado (fullName, lastLogin)
        User user = a.getUser();
        if (user != null) {
            if (dto.getFullName() != null) {
                user.setFullName(dto.getFullName());
            }
            userRepo.save(user); // guardamos nombre actualizado antes de sincronizar credenciales
        }

        // ---- Manejo simple de admission_results ----
        boolean hasAdmissionFields = dto.getCareerAtResult() != null
                || dto.getScore() != null
                || dto.getAdmissionYear() != null;

        if (hasAdmissionFields) {
            if (hasAdmissionFields) {
            // Determinar año objetivo (si no viene, usamos el admissionYear del applicant)
            Integer targetYear = dto.getAdmissionYear() != null ? dto.getAdmissionYear() : a.getAdmissionYear();
            // Buscar el último resultado
            AdmissionResult last = admissionResultRepo.findTopByApplicantOrderByCreatedAtDesc(a).orElse(null);

            if (last != null && last.getAdmissionYear() != null && last.getAdmissionYear().equals(targetYear)) {
                if (dto.getCareerAtResult() != null)
                    last.setCareerAtResult(dto.getCareerAtResult());
                if (dto.getScore() != null)
                    last.setScore(dto.getScore());
                if (dto.getAdmissionYear() != null)
                    last.setAdmissionYear(dto.getAdmissionYear());
                admissionResultRepo.save(last);
            } else {
                AdmissionResult newRes = new AdmissionResult();
                newRes.setApplicant(a);
                newRes.setCareerAtResult(dto.getCareerAtResult());
                newRes.setScore(dto.getScore());
                newRes.setAdmissionYear(targetYear);
                admissionResultRepo.save(newRes);
            }
        }
        }

        Applicant saved = applicantRepo.save(a);
        
        // --- Aquí sincronizamos las credenciales del usuario ---
        try {
            userService.syncUserCredentialsForApplicant(saved);
        } catch (RuntimeException ex) {
            // Log y convertir en ResponseStatusException para retornarlo al cliente admin
            logger.error("Error sincronizando credenciales para applicant id {}: {}", saved.getId(), ex.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se pudieron sincronizar credenciales: " + ex.getMessage());
        }

        return toDto(saved);
    }

    // toDto existente (asegúrate que incluye fullName/lastLogin ya que proviene de
    // user)
    private ApplicantResponseDTO toDto(Applicant a) {
        ApplicantResponseDTO dto = new ApplicantResponseDTO();
        dto.setId(a.getId());
        dto.setFicha(a.getFicha());
        dto.setCurp(a.getCurp());
        dto.setCareerAtResult(a.getCareerAtResult());
        dto.setFullName(a.getUser() != null ? a.getUser().getFullName() : null);
        dto.setCareer(a.getCareer());
        dto.setLocation(a.getLocation());
        dto.setExamRoom(a.getExamRoom());
        dto.setExamDate(a.getExamDate());
        dto.setStatus(a.getStatus());
        dto.setAdmissionYear(a.getAdmissionYear());
        dto.setLastLogin(a.getUser() != null ? a.getUser().getLastLogin() : null);
        return dto;
    }
}