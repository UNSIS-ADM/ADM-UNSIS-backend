package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.*;
import com.unsis.admunsisbackend.model.*;
import com.unsis.admunsisbackend.repository.*;
import com.unsis.admunsisbackend.service.CareerChangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CareerChangeServiceImpl implements CareerChangeService {
    @Autowired
    private ApplicantRepository applicantRepo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private CareerChangeRequestRepository reqRepo;

    @Override
    @Transactional
    public CareerChangeRequestDTO submitChange(String username, CreateCareerChangeRequestDTO dto) {
        // 1) Buscar el User y su Applicant
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));  // si no es aspirante, error

        // 2) Verificar que NO tenga ya una solicitud PENDING
        Applicant app = user.getApplicant();
        if (app == null)
            throw new RuntimeException("No es aspirante");
        // Comprueba que no tenga ya una solicitud PENDING
        reqRepo.findByApplicantAndStatus(app, "PENDING")
                .ifPresent(r -> {
                    throw new RuntimeException("Ya hay una solicitud pendiente");
                });
        
        // 3) Crear y guardar la solicitud
        CareerChangeRequest r = new CareerChangeRequest();
        r.setApplicant(app);
        r.setOldCareer(app.getCareer());
        r.setNewCareer(dto.getNewCareer());
        r.setRequestComment(dto.getRequestComment());
        r.setStatus("PENDING");
        r = reqRepo.save(r);
        return toDto(r);
    }

    @Override
    public List<CareerChangeRequestDTO> listPending() {
        return reqRepo.findByStatus("PENDING")
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CareerChangeRequestDTO processRequest(Long requestId, ProcessCareerChangeRequestDTO dto,
            String adminUsername) {
        // 1) Cargar la solicitud
        CareerChangeRequest r = reqRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        if (!"PENDING".equals(r.getStatus())) {
            throw new RuntimeException("Solicitud ya procesada");
        }
        // 2) Cargar el admin/secretaria que procesa
        User admin = userRepo.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Admin no encontrado"));

        // 3) Guardar comentarios y quién procesó
        r.setResponseComment(dto.getResponseComment());
        r.setProcessedAt(LocalDateTime.now());
        r.setProcessedBy(admin);

        if ("APPROVE".equalsIgnoreCase(dto.getAction())) {
            // 4a) Aprobar: actualizar Applicant.career y Applicant.status
            r.setStatus("APPROVED");
            // Actualiza carrera y status en Applicant
            Applicant a = r.getApplicant();
            a.setCareer(r.getNewCareer());
            a.setStatus("ACEPTADO");
            applicantRepo.save(a);
        } else {
            // 4b) Rechazar: solo actualizar status
            r.setStatus("DENIED");
        }
        return toDto(reqRepo.save(r));
    }

    private CareerChangeRequestDTO toDto(CareerChangeRequest r) {
        CareerChangeRequestDTO d = new CareerChangeRequestDTO();
        d.setId(r.getId());
        d.setApplicantId(r.getApplicant().getId());
        d.setOldCareer(r.getOldCareer());
        d.setNewCareer(r.getNewCareer());
        d.setStatus(r.getStatus());
        d.setRequestComment(r.getRequestComment());
        d.setResponseComment(r.getResponseComment());
        d.setRequestedAt(r.getRequestedAt());
        d.setProcessedAt(r.getProcessedAt());
        d.setProcessedBy(r.getProcessedBy() != null ? r.getProcessedBy().getUsername() : null);
        return d;
    }
}
