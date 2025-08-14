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

    @Autowired private ApplicantRepository applicantRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private VacancyRepository vacancyRepo;
    @Autowired private CareerChangeRequestRepository reqRepo;

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

        vac.setAvailableSlots(vac.getAvailableSlots() - 1); // Reservar un slot
        vacancyRepo.save(vac); // Guardar la vacante actualizada

        // 5) Crear solicitud
        CareerChangeRequest solicitud = new CareerChangeRequest();
        solicitud.setApplicant(app);
        solicitud.setOldCareer(app.getCareer());
        solicitud.setNewCareer(nuevaCarrera);
        solicitud.setRequestComment(dto.getRequestComment());
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
//Inico de INTEGRACION
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
        Vacancy vac = vacancyRepo.findByCareerAndAdmissionYear(nuevaCarrera, año)
                .orElseThrow(() -> new RuntimeException("Vacantes no configuradas para " +  nuevaCarrera));

        // Procesar
        solicitud.setResponseComment(dto.getResponseComment());
        solicitud.setProcessedAt(LocalDateTime.now());
        solicitud.setProcessedBy(admin);

        // Acción en español: dto.getAction() = "ACEPTADO" o "RECHAZADO"
        String accion = dto.getAction() != null ? dto.getAction().trim().toUpperCase() : "";

        if ("ACEPTADO".equalsIgnoreCase(dto.getAction())) {
            // Cambiar a ACEPTADO
            // Aprobar: consumimos la reserva (ya fue descontada en submitChange),
            // actualizamos applicant.career y applicant.status a "ACEPTADO".
            solicitud.setStatus("ACEPTADO");
            app.setCareer(nuevaCarrera);
            app.setStatus("ACEPTADO");
            applicantRepo.save(app);
            // Opcional: recalcular counters de vacante (accepted_count, available_slots)
            // vacancyService.recalculateOne(nuevaCarrera, año); // si tienes ese servicio
        }
        else {
            // Rechazar: devolver la vacante reservada y marcar al applicant con estatus
            // "SOLICITUD RECHAZADA"
            solicitud.setStatus("RECHAZADO"); // o "SOLICITUD RECHAZADA" si prefieres texto largo
            app.setStatus("SOLICITUD RECHAZADA, pero sigues en inscrito en " + app.getCareer());
            applicantRepo.save(app);

            String oldCareer = solicitud.getOldCareer();
            int year = solicitud.getApplicant().getAdmissionYear();
            Vacancy oldCareerVacancy = vacancyRepo
                    .findByCareerAndAdmissionYear(oldCareer, year)
                    .orElseThrow(() -> new RuntimeException(
                            "Vacantes no configuradas para " + oldCareer + " en " + year));

            // Reincrementamos la vacante que habíamos reservado al crear la solicitud
            oldCareerVacancy.setAvailableSlots(oldCareerVacancy.getAvailableSlots() + 1);
            vacancyRepo.save(oldCareerVacancy);
        }
        CareerChangeRequest saved = reqRepo.save(solicitud);
        return toDto(saved);
    }

    private CareerChangeRequestDTO toDto(CareerChangeRequest r) {
        CareerChangeRequestDTO d = new CareerChangeRequestDTO();
        d.setId(r.getId());
        d.setApplicantId(r.getApplicant().getId());
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
}



