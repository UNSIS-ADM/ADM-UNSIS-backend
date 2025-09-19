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
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Applicant app = user.getApplicant();
        if (app == null)
            throw new RuntimeException("No es aspirante");

        String nuevaCarrera = dto.getNewCareer().trim();
        if ("LICENCIATURA EN MEDICINA".equalsIgnoreCase(nuevaCarrera)) {
            throw new RuntimeException("No está permitido postularse a Medicina");
        }
        String carreraActual = app.getCareer() != null ? app.getCareer().trim() : "";
        if (carreraActual.equalsIgnoreCase(nuevaCarrera)) {
            throw new RuntimeException("No puedes solicitar cambio a la misma carrera");
        }

        boolean yaSolicito = reqRepo.findByApplicant(app).stream().findAny().isPresent();
        if (yaSolicito) {
            throw new RuntimeException("Ya realizaste una solicitud de cambio de carrera");
        }

        int año = app.getAdmissionYear();

        // --- LOCK para evitar race conditions al reservar ---
        Vacancy vac = vacancyRepo.findByCareerAndAdmissionYearForUpdate(nuevaCarrera, año)
                .orElseThrow(() -> new RuntimeException("Vacantes no configuradas para " + nuevaCarrera));

        int availableNow = Optional.ofNullable(vac.getAvailableSlots()).orElse(0);
        if (availableNow <= 0) {
            throw new RuntimeException("Cupo agotado para " + nuevaCarrera);
        }

        // Reservar: incrementar reservedCount y recalcular available
        int reservedNow = Optional.ofNullable(vac.getReservedCount()).orElse(0);
        vac.setReservedCount(reservedNow + 1);

        // recalcula availableSlots = cuposInserted - inscritosCount - reservedCount
        int cupos = Optional.ofNullable(vac.getCuposInserted()).orElse(0);
        int inscritos = Optional.ofNullable(vac.getInscritosCount()).orElse(0);
        int newAvailable = Math.max(0, cupos - inscritos - vac.getReservedCount());
        vac.setAvailableSlots(newAvailable);

        vacancyRepo.save(vac);

        // Crear solicitud (no tocar vacantes origen)
        CareerChangeRequest solicitud = new CareerChangeRequest();
        solicitud.setApplicant(app);
        solicitud.setOldCareer(app.getCareer());
        solicitud.setNewCareer(nuevaCarrera);
        solicitud.setRequestComment(dto.getRequestComment());
        solicitud.setResponseComment("SOLICITUD ENVIADA");
        solicitud.setOldStatus(app.getStatus());
        solicitud.setStatus("PENDIENTE");
        solicitud = reqRepo.save(solicitud);

        // Marcar al aspirante como "Solicitud en proceso"
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

        User admin = userRepo.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Applicant app = solicitud.getApplicant();
        String nuevaCarrera = solicitud.getNewCareer();
        int año = app.getAdmissionYear();

        // lock destino para actualizar reserved/inscritos/available de forma atómica
        Vacancy vacNueva = vacancyRepo.findByCareerAndAdmissionYearForUpdate(nuevaCarrera, año)
                .orElseThrow(() -> new RuntimeException("Vacantes no configuradas para " + nuevaCarrera));

        solicitud.setResponseComment(dto.getResponseComment());
        solicitud.setProcessedAt(LocalDateTime.now());
        solicitud.setProcessedBy(admin);

        String accion = dto.getAction() != null ? dto.getAction().trim().toUpperCase() : "";

        // En ambos casos vamos a consumir la reserva (reservedCount--)
        int reservedNow = Optional.ofNullable(vacNueva.getReservedCount()).orElse(0);
        vacNueva.setReservedCount(Math.max(0, reservedNow - 1));

        // Y en ambos casos incrementamos inscritosCount (policy que solicitaste)
        int inscritosNow = Optional.ofNullable(vacNueva.getInscritosCount()).orElse(0);
        vacNueva.setInscritosCount(inscritosNow + 1);

        // Recalcular available
        int cupos = Optional.ofNullable(vacNueva.getCuposInserted()).orElse(0);
        int availableAfter = Math.max(0, cupos - vacNueva.getInscritosCount() - vacNueva.getReservedCount());
        vacNueva.setAvailableSlots(availableAfter);

        if ("ACEPTADO".equalsIgnoreCase(accion)) {
            solicitud.setStatus("ACEPTADO");

            // mover applicant a nueva carrera y marcar aceptado
            String oldCareer = app.getCareer();
            app.setCareer(nuevaCarrera);
            app.setStatus("ACEPTADO");
            applicantRepo.save(app);

            // guardamos cambios en vacancy destino
            vacancyRepo.save(vacNueva);

            // NO liberar vacante en carrera origen (según tu regla)
            // Si deseas actualizar counters de la carrera origen (p.ej. disminuir
            // inscritos),
            // aquí NO lo hacemos (policy: no devolver).
        } else if ("RECHAZADO".equalsIgnoreCase(accion)) {
            solicitud.setStatus("RECHAZADO");
            app.setStatus("RECHAZADO");
            applicantRepo.save(app);

            // Aunque rechazado, la política dice "sumarlo a la carrera solicitada"
            vacancyRepo.save(vacNueva);

            // NO devolvemos reserva ni modificamos carrera origen.
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

