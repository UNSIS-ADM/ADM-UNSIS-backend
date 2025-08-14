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

        vac.setAvailableSlots(vac.getAvailableSlots() - 1); // Reservar un slot
        vacancyRepo.save(vac); // Guardar la vacante actualizada

        // 5) Crear solicitud
        CareerChangeRequest solicitud = new CareerChangeRequest();
        solicitud.setApplicant(app);
        solicitud.setOldCareer(app.getCareer());
        solicitud.setNewCareer(nuevaCarrera);
        solicitud.setRequestComment(dto.getRequestComment());
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

        if ("ACEPTADO".equalsIgnoreCase(dto.getAction())) {
            // ACEPTAR: consume la reserva ya hecha en submit (no tocar availableSlots)
            solicitud.setStatus("ACEPTADO");

            app.setCareer(nuevaCarrera); // cambia a la nueva carrera
            app.setStatus("ACEPTADO"); // siemore queda aceptado
            applicantRepo.save(app);

        } else if ("RECHAZADO".equals(accion))

        {
            // RECHAZAR: retorna la reserva a la NUEVA carrera
            solicitud.setStatus("RECHAZADO");

            // Restaurar el status previo (CASO 1 y 4)
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

        } else {
            throw new RuntimeException("Acción inválida. Usa 'ACEPTADO' o 'RECHAZADO'.");
        }

        CareerChangeRequest saved = reqRepo.save(solicitud);
        return

        toDto(saved);
    }
    /*
     * else {
     * // Rechazar: devolver la vacante reservada y marcar al applicant con estatus
     * // "SOLICITUD RECHAZADA"
     * solicitud.setStatus("RECHAZADO"); // o "SOLICITUD RECHAZADA" si prefieres
     * texto largo
     * app.setStatus("SOLICITUD RECHAZADA, pero sigues en inscrito en " +
     * app.getCareer());
     * applicantRepo.save(app);
     * 
     * String oldCareer = solicitud.getOldCareer();
     * int year = solicitud.getApplicant().getAdmissionYear();
     * Vacancy oldCareerVacancy = vacancyRepo
     * .findByCareerAndAdmissionYear(oldCareer, year)
     * .orElseThrow(() -> new RuntimeException(
     * "Vacantes no configuradas para " + oldCareer + " en " + year));
     * 
     * // Reincrementamos la vacante que habíamos reservado al crear la solicitud
     * oldCareerVacancy.setAvailableSlots(oldCareerVacancy.getAvailableSlots() + 1);
     * vacancyRepo.save(oldCareerVacancy);
     * }
     * CareerChangeRequest saved = reqRepo.save(solicitud);
     * return toDto(saved);
     * }
     */

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


/*
 * 102 SBHB010228HOCNRLA3 ANA TORRES JARQUIN LICENCIATURA EN MEDICINA ACEPTADO
 * -> ACEPTADO
 * 
 * 103 SBHB010228HOCNRLA4 PEDRO AQUINO CANSECO LICENCIATURA EN ODONTOLOGÍA
 * ACEPTADO -> RECHAZADO
 * 
 * 115 JAHB010228HOCNRLB7 ALCÁNTARA PÉREZ CARLOS LICENCIATURA EN APÚBLICA
 * RECHAZADO -> ACEPTADO
 * 
 * 118 THHB010228HOCNRLC1 CASIMIRA REYES LOPEZ LICENCIATURA EN MEDICINA
 * RECHAZADO -> RECHAZADO
 * 
 * Hay un pequeño error porque esta permitiendo solicitar su cambio a las misma
 * carrera odonto a odonto,
 * Ademas Explicame donde debo de exponer el endpoint para que el aspirante vea su resultado de su solicitud
 * 
 * Tambien si desde el front se manda en minuscula no acepta la solicitud.
 * Carlos arreglar los alerts, por otros mensajes.
 * 
 * Id Ficha Carrera adscrita Carrera Solicitada Comentario del aspirante Estado
 * Acciones
 * 1 102 LICENCIATURA EN MEDICINA       LICENCIATURA EN CIENCIAS BIOMÉDICAS Ejemplo de aceptado a aceptado Pendiente Atender
 * 2 103 LICENCIATURA EN ODONTOLOGÍA    LICENCIATURA EN ENFERMERÍA Ejemplo de aceptado a rechazado Pendiente Atender
 * 3 115 LICENCIATURA EN ADM PÚBLICA    LICENCIATURA EN NUTRICION ejemplo de rechazado a aceptado Pendiente Atender
 * 4 118 LICENCIATURA EN MEDICINA       LICENCIATURA EN INFORMATICA ejemplo de rechazado a rechazado Pendiente Atender
 * 5 100 LICENCIATURA EN ENFERMERÍA     LICENCIATURA EN ENFERMERÍA Cambio a la misma carrera Pendiente Atender
 */