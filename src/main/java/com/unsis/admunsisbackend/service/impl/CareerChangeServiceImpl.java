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

        // Reservar: incrementar pendingCount y recalcular availableSlots
        int pendingNow = vac.getPendingCount() != null ? vac.getPendingCount() : 0;
        vac.setPendingCount(pendingNow + 1);

        int acceptedNow = vac.getAcceptedCount() != null ? vac.getAcceptedCount() : 0;
        int rejectedNow = vac.getRejectedCount() != null ? vac.getRejectedCount() : 0;
        int totalDecisions = acceptedNow + rejectedNow;
        int limit = vac.getLimitCount() != null ? vac.getLimitCount() : 0;

        int newAvailable = Math.max(0, limit - totalDecisions - (pendingNow + 1));
        vac.setAvailableSlots(newAvailable);

        vacancyRepo.save(vac);

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
            // ACEPTAR: consume la reserva ya hecha en submit
            solicitud.setStatus("ACEPTADO");

            app.setCareer(nuevaCarrera); // cambia a la nueva carrera
            app.setStatus("ACEPTADO");
            applicantRepo.save(app);

            // actualizar vacante: pending--, accepted++, total++
            int acceptedCount = vacNueva.getAcceptedCount() != null ? vacNueva.getAcceptedCount() : 0;
            int pendingCount = vacNueva.getPendingCount() != null ? vacNueva.getPendingCount() : 0;
            int rejectedCount = vacNueva.getRejectedCount() != null ? vacNueva.getRejectedCount() : 0;

            vacNueva.setPendingCount(Math.max(0, pendingCount - 1));
            vacNueva.setAcceptedCount(acceptedCount + 1);
            vacNueva.setTotalCount((acceptedCount + 1) + rejectedCount);

            // recalcular availableSlots = limit - total - pending
            int limit = vacNueva.getLimitCount() != null ? vacNueva.getLimitCount() : 0;
            int availableAfter = Math.max(0, limit - vacNueva.getTotalCount() - vacNueva.getPendingCount());
            vacNueva.setAvailableSlots(availableAfter);

            vacancyRepo.save(vacNueva);
        } else if ("RECHAZADO".equalsIgnoreCase(accion))

        {
            // RECHAZAR: retorna la reserva a la NUEVA carrera
            solicitud.setStatus("RECHAZADO");

            /*/ Restaurar el status previo (CASO 1 y 4)
            String previo = solicitud.getOldStatus();
            if ("ACEPTADO".equalsIgnoreCase(previo)) {
                app.setStatus("ACEPTADO"); // CASO 1
            } else if ("RECHAZADO".equalsIgnoreCase(previo)) {
                app.setStatus("RECHAZADO"); // CASO 4
            } else {
                app.setStatus("RECHAZADO"); // por defecto si no hubiera dato
            }
            // La carrera NO cambia en rechazo: se queda en su carrera original (oldCareer).

            // Devolver la reserva a la NUEVA carrera
            vacNueva.setAvailableSlots(vacNueva.getAvailableSlots() + 1);
            vacancyRepo.save(vacNueva);

            applicantRepo.save(app);
            */

            // No restauramos applicant.status al previo, sino que lo marcamos RECHAZADO
            // (según tu requerimiento de no restaurar)
            app.setStatus("RECHAZADO");
            applicantRepo.save(app);

            // No incrementamos vacNueva.availableSlots (la reserva NO se devuelve)
            // vacancyRepo.save(vacNueva); // <-- intencionalmente NO lo hacemos

            // actualizar vacante: pending--, rejected++, total++
            int acceptedCount = vacNueva.getAcceptedCount() != null ? vacNueva.getAcceptedCount() : 0;
            int pendingCount = vacNueva.getPendingCount() != null ? vacNueva.getPendingCount() : 0;
            int rejectedCount = vacNueva.getRejectedCount() != null ? vacNueva.getRejectedCount() : 0;

            vacNueva.setPendingCount(Math.max(0, pendingCount - 1));
            vacNueva.setRejectedCount(rejectedCount + 1);
            vacNueva.setTotalCount(acceptedCount + (rejectedCount + 1));

            // NO incrementamos availableSlots; recalculamos availableSlots = limit - total
            // - pending (esto reflejará que no devuelve cupo)
            int limit = vacNueva.getLimitCount() != null ? vacNueva.getLimitCount() : 0;
            int availableAfter = Math.max(0, limit - vacNueva.getTotalCount() - vacNueva.getPendingCount());
            vacNueva.setAvailableSlots(availableAfter);

            vacancyRepo.save(vacNueva);

        } else {
            throw new RuntimeException("Acción inválida. Usa 'ACEPTADO' o 'RECHAZADO'.");
        }

        CareerChangeRequest saved = reqRepo.save(solicitud);
        return

        toDto(saved);
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
}

