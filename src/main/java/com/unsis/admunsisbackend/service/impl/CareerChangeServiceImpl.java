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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CareerChangeServiceImpl implements CareerChangeService {

    @Autowired
    private ApplicantRepository applicantRepo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private VacancyRepository vacancyRepo;
    @Autowired
    private CareerChangeRequestRepository reqRepo;

    @Override
    @Transactional
    public CareerChangeRequestDTO submitChange(String username, CreateCareerChangeRequestDTO dto) {
        // 1) Obtener usuario y aspirante
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Applicant app = user.getApplicant();
        if (app == null)
            throw new RuntimeException("No es aspirante");

        // 2) Prohibir postularse a Medicina
        String nuevaCarrera = dto.getNewCareer().trim();
        if ("LICENCIATURA EN MEDICINA".equalsIgnoreCase(nuevaCarrera)) {
            throw new RuntimeException("No está permitido postularse a Medicina");
        }
        // NUEVA validación: no permitir solicitar a la misma carrera
        String carreraActual = app.getCareer() != null ? app.getCareer().trim() : "";
        if (carreraActual.equalsIgnoreCase(nuevaCarrera)) {
            throw new RuntimeException("No puedes solicitar cambio a la misma carrera");
        }

        // 3) Única solicitud en toda la vida del aspirante
        boolean yaSolicito = reqRepo.findByApplicant(app).stream().findAny().isPresent();
        if (yaSolicito) {
            throw new RuntimeException("Ya realizaste una solicitud de cambio de carrera");
        }

        // 4) Validar vacantes disponibles (sin modificar limit_count)
        int año = app.getAdmissionYear();
        Vacancy vac = vacancyRepo.findByCareerAndAdmissionYear(nuevaCarrera, año)
                .orElseThrow(() -> new RuntimeException("Vacantes no configuradas para" + nuevaCarrera));
        if (vac.getAvailableSlots() <= 0) {
            throw new RuntimeException("Cupo agotado para " + nuevaCarrera);
        }


        // 5) Crear solicitud
        CareerChangeRequest solicitud = new CareerChangeRequest();
        solicitud.setApplicant(app);
        solicitud.setOldCareer(app.getCareer());
        solicitud.setNewCareer(nuevaCarrera);
        solicitud.setRequestComment(dto.getRequestComment());
        solicitud.setResponseComment("SOLICITUD ENVIADA");
        solicitud.setOldStatus(app.getStatus());
        solicitud.setStatus("PENDIENTE");
        solicitud = reqRepo.save(solicitud);

        // 6) Marcar al aspirante como "Solicitud en proceso"
        app.setStatus("Solicitud en proceso");
        applicantRepo.save(app);

        return toDto(solicitud);
    }

    @Override
    public List<CareerChangeRequestDTO> listPending() {
        return reqRepo.findByStatus("PENDIENTE")

                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // Inico de INTEGRACION
    @Override
    @Transactional
    public CareerChangeRequestDTO processRequest(Long requestId, ProcessCareerChangeRequestDTO dto,
            String adminUsername) {
        CareerChangeRequest solicitud = reqRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        if (!"PENDIENTE".equals(solicitud.getStatus())) {
            throw new RuntimeException("Solicitud ya procesada");
        }

        // Cargar admin/secretaria
        User admin = userRepo.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Applicant app = solicitud.getApplicant();
        String nuevaCarrera = solicitud.getNewCareer();
        int año = app.getAdmissionYear();

        // Re-verificar vacantes (en caso de race condition)
        Vacancy vacNueva = vacancyRepo.findByCareerAndAdmissionYear(nuevaCarrera, año)
                .orElseThrow(() -> new RuntimeException("Vacantes no configuradas para " + nuevaCarrera));

        // Procesar
        solicitud.setResponseComment(dto.getResponseComment());
        solicitud.setProcessedAt(LocalDateTime.now());
        solicitud.setProcessedBy(admin);

        // Acción en español: dto.getAction() = "ACEPTADO" o "RECHAZADO"
        String accion = dto.getAction() != null ? dto.getAction().trim().toUpperCase() : "";

        if ("ACEPTADO".equalsIgnoreCase(accion)) {
            // Re-verificar disponibilidad atómica (vacNueva está for update)
            int availableNow = Optional.ofNullable(vacNueva.getAvailableSlots()).orElse(0);
            if (availableNow <= 0) {
                throw new RuntimeException("Ya no hay cupos disponibles para " + nuevaCarrera);
            }

            // Aceptar: mover al applicant a la nueva carrera
            solicitud.setStatus("ACEPTADO");

            // Guarda carrera anterior para recalcular su vacante después
            String oldCareer = app.getCareer();

            app.setCareer(nuevaCarrera);
            app.setStatus("ACEPTADO");
            applicantRepo.save(app);

            // Recalcular vacantes para carrera destino y origen (si existe)
            recalculateVacancyFromApplicants(nuevaCarrera, año);
            if (oldCareer != null && !oldCareer.trim().isEmpty()) {
                recalculateVacancyFromApplicants(oldCareer, año);
            }

            vacancyRepo.flush(); // opcional: forzar sincronización
        } else if ("RECHAZADO".equalsIgnoreCase(accion)) {
            // Rechazar: no devolvemos cupo, no tocamos vacantes (según tu política)
            solicitud.setStatus("RECHAZADO");
            app.setStatus("RECHAZADO");
            applicantRepo.save(app);
        } else {
            throw new RuntimeException("Acción inválida. Usa 'ACEPTADO' o 'RECHAZADO'.");
        }

        CareerChangeRequest saved = reqRepo.save(solicitud);
        return toDto(saved);
    }
    
    private CareerChangeRequestDTO toDto(CareerChangeRequest r) {
        CareerChangeRequestDTO d = new CareerChangeRequestDTO();
        d.setId(r.getId());
        d.setApplicantId(r.getApplicant().getId());
        d.setFullName(r.getApplicant().getUser().getFullName());
        d.setFicha(r.getApplicant() != null ? r.getApplicant().getFicha() : null);
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

    @Override
    public List<CareerChangeRequestDTO> listByUsername(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Applicant app = user.getApplicant();
        if (app == null)
            return List.of();
        return reqRepo.findByApplicant(app).stream().map(this::toDto).collect(Collectors.toList());
    }
    
    /**
     * Recalcula inscritosCount y availableSlots para una carrera y año
     * basándose en el conteo real de applicants.
     */
    private void recalculateVacancyFromApplicants(String career, int year) {
        Vacancy vac = vacancyRepo.findByCareerAndAdmissionYear(career, year)
                .orElseGet(() -> {
                    Vacancy nv = new Vacancy();
                    nv.setCareer(career);
                    nv.setAdmissionYear(year);
                    nv.setCuposInserted(0);
                    nv.setInscritosCount(0);
                    nv.setAvailableSlots(0);
                    return nv;
                });

        long inscritos = applicantRepo.countByCareerAndAdmissionYear(career, year);
        vac.setInscritosCount((int) inscritos);

        int cupos = Optional.ofNullable(vac.getCuposInserted()).orElse(0);
        int available = Math.max(0, cupos - (int) inscritos);
        vac.setAvailableSlots(available);

        vacancyRepo.save(vac);
    }

}

