package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.*;
import com.unsis.admunsisbackend.model.*;
import com.unsis.admunsisbackend.repository.*;
import com.unsis.admunsisbackend.service.CareerChangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
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

        // Reservar: incrementar reservedCount y recalcular available usando la nueva
        // fórmula
        int reservedNow = Optional.ofNullable(vac.getReservedCount()).orElse(0);
        vac.setReservedCount(reservedNow + 1);

        int cupos = Optional.ofNullable(vac.getCuposInserted()).orElse(0);
        int inscritos = Optional.ofNullable(vac.getInscritosCount()).orElse(0);
        int released = Optional.ofNullable(vac.getReleasedCount()).orElse(0);
        int newAvailable = Math.max(0, cupos - inscritos - vac.getReservedCount() - released);
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

    // Inicio de INTEGRACION
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
        String oldCareer = app.getCareer(); // carrera origen (puede ser null)
        int año = app.getAdmissionYear();

        solicitud.setResponseComment(dto.getResponseComment());
        solicitud.setProcessedAt(LocalDateTime.now());
        solicitud.setProcessedBy(admin);

        String accion = dto.getAction() != null ? dto.getAction().trim().toUpperCase() : "";

        // --- 1) Lock vacantes en orden determinista para evitar deadlocks ---
        List<String> carrerasToLock = new ArrayList<>();
        if (oldCareer != null && !oldCareer.trim().isEmpty())
            carrerasToLock.add(oldCareer.trim());
        if (nuevaCarrera != null && !nuevaCarrera.trim().isEmpty())
            carrerasToLock.add(nuevaCarrera.trim());

        carrerasToLock = carrerasToLock.stream().distinct().sorted().collect(Collectors.toList());

        Map<String, Vacancy> locked = new HashMap<>();
        for (String c : carrerasToLock) {
            Vacancy vac = vacancyRepo.findByCareerAndAdmissionYearForUpdate(c, año)
                    .orElseGet(() -> {
                        Vacancy nv = new Vacancy();
                        nv.setCareer(c);
                        nv.setAdmissionYear(año);
                        nv.setCuposInserted(0);
                        nv.setInscritosCount(0);
                        nv.setReservedCount(0);
                        nv.setAvailableSlots(0);
                        nv.setReleasedCount(0);
                        return nv;
                    });
            locked.put(c, vac);
        }

        Vacancy vacNueva = locked.get(nuevaCarrera);
        if (vacNueva == null) {
            throw new RuntimeException("Vacantes no configuradas para " + nuevaCarrera);
        }

        // --- 2) Consumir la reserva en destino ---
        int reservedNow = Optional.ofNullable(vacNueva.getReservedCount()).orElse(0);
        vacNueva.setReservedCount(Math.max(0, reservedNow - 1));

        int inscritosNowDestino = Optional.ofNullable(vacNueva.getInscritosCount()).orElse(0);
        vacNueva.setInscritosCount(inscritosNowDestino + 1);

        int cuposDestino = Optional.ofNullable(vacNueva.getCuposInserted()).orElse(0);
        int releasedDestino = Optional.ofNullable(vacNueva.getReleasedCount()).orElse(0);
        int availableAfterDestino = Math.max(0,
                cuposDestino - Optional.ofNullable(vacNueva.getInscritosCount()).orElse(0)
                        - Optional.ofNullable(vacNueva.getReservedCount()).orElse(0)
                        - releasedDestino);
        vacNueva.setAvailableSlots(availableAfterDestino);

        // --- 3) Si hay carrera origen y la acción es ACEPTADO, decrementar inscritos
        // en origen
        // y registrar el decremento en releasedCount (sin aumentar availableSlots) ---
        Vacancy vacOrigen = (oldCareer != null && !oldCareer.trim().isEmpty())
                ? locked.get(oldCareer.trim())
                : null;

        if ("ACEPTADO".equalsIgnoreCase(accion)) {
            solicitud.setStatus("ACEPTADO");

            // mover applicant a nueva carrera y marcar aceptado
            app.setCareer(nuevaCarrera);
            app.setStatus("ACEPTADO");
            applicantRepo.save(app);

            // Decrementar inscritos en la vacante origen si existe
            if (vacOrigen != null) {
                int inscritosOrigNow = Optional.ofNullable(vacOrigen.getInscritosCount()).orElse(0);
                vacOrigen.setInscritosCount(Math.max(0, inscritosOrigNow - 1));

                // registrar la liberación en released_count (no mostrar en front)
                int prevReleased = Optional.ofNullable(vacOrigen.getReleasedCount()).orElse(0);
                vacOrigen.setReleasedCount(prevReleased + 1);

                // Recalcular available usando la fórmula que incluye released_count, de modo
                // que
                // la disminución de inscritos quede compensada por released_count y available
                // no cambie.
                int cuposOrig = Optional.ofNullable(vacOrigen.getCuposInserted()).orElse(0);
                int reservedOrig = Optional.ofNullable(vacOrigen.getReservedCount()).orElse(0);
                int releasedOrig = Optional.ofNullable(vacOrigen.getReleasedCount()).orElse(0);
                int availableAfterOrig = Math.max(0,
                        cuposOrig - vacOrigen.getInscritosCount() - reservedOrig - releasedOrig);
                vacOrigen.setAvailableSlots(availableAfterOrig);

                vacancyRepo.save(vacOrigen);
            }

            // Guardamos cambios en vacancy destino
            vacancyRepo.save(vacNueva);

        } else if ("RECHAZADO".equalsIgnoreCase(accion)) {
            solicitud.setStatus("RECHAZADO");
            app.setStatus("RECHAZADO");
            applicantRepo.save(app);

            // Además de mantener el incremento en destino (policy actual),
            // también decrementamos inscritos en la carrera origen y registramos
            // released_count.
            if (vacOrigen != null) {
                int inscritosOrigNow = Optional.ofNullable(vacOrigen.getInscritosCount()).orElse(0);
                vacOrigen.setInscritosCount(Math.max(0, inscritosOrigNow - 1));

                int prevReleased = Optional.ofNullable(vacOrigen.getReleasedCount()).orElse(0);
                vacOrigen.setReleasedCount(prevReleased + 1);

                int cuposOrig = Optional.ofNullable(vacOrigen.getCuposInserted()).orElse(0);
                int reservedOrig = Optional.ofNullable(vacOrigen.getReservedCount()).orElse(0);
                int releasedOrig = Optional.ofNullable(vacOrigen.getReleasedCount()).orElse(0);
                int availableAfterOrig = Math.max(0,
                        cuposOrig - vacOrigen.getInscritosCount() - reservedOrig - releasedOrig);
                vacOrigen.setAvailableSlots(availableAfterOrig);

                vacancyRepo.save(vacOrigen);
            }

            // Política actual: aun en RECHAZADO guardas en destino el incremento
            vacancyRepo.save(vacNueva);

            // No tocamos origen en caso de rechazo (según policy antiguo). Ahora sí lo
            // actualizamos.
        } else {
            throw new RuntimeException("Acción inválida. Usa 'ACEPTADO' o 'RECHAZADO'.");
        }

        // Persistir la solicitud finalmente
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
     * basándose en el conteo real de applicants; la fórmula incorpora releasedCount
     * para mantener la consistencia con la nueva política.
     */
    private void recalculateVacancyFromApplicants(String career, int year) {
        Vacancy vac = vacancyRepo.findByCareerAndAdmissionYear(career, year)
                .orElseGet(() -> {
                    Vacancy nv = new Vacancy();
                    nv.setCareer(career);
                    nv.setAdmissionYear(year);
                    nv.setCuposInserted(0);
                    nv.setInscritosCount(0);
                    nv.setReservedCount(0);
                    nv.setReleasedCount(0);
                    nv.setAvailableSlots(0);
                    return nv;
                });

        long inscritos = applicantRepo.countByCareerAndAdmissionYear(career, year);
        vac.setInscritosCount((int) inscritos);

        int cupos = Optional.ofNullable(vac.getCuposInserted()).orElse(0);
        int reserved = Optional.ofNullable(vac.getReservedCount()).orElse(0);
        int released = Optional.ofNullable(vac.getReleasedCount()).orElse(0);

        int available = Math.max(0, cupos - (int) inscritos - reserved - released);
        vac.setAvailableSlots(available);

        vacancyRepo.save(vac);
    }

}


/*
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
     *
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
 */

