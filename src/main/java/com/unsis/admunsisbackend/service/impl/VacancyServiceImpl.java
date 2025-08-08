package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.VacancyDTO;
import com.unsis.admunsisbackend.model.Vacancy;
import com.unsis.admunsisbackend.repository.ApplicantRepository;
import com.unsis.admunsisbackend.repository.CareerChangeRequestRepository;
import com.unsis.admunsisbackend.repository.VacancyRepository;
import com.unsis.admunsisbackend.service.VacancyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VacancyServiceImpl implements VacancyService {

    @Autowired
    private VacancyRepository vacancyRepo;

    @Autowired
    private ApplicantRepository applicantRepo;

    @Autowired
    private CareerChangeRequestRepository requestRepo;

    @Override
    public List<VacancyDTO> listVacancies(Integer year) {
        int y = (year != null ? year : Year.now().getValue());
        return vacancyRepo.findByAdmissionYear(y)
                .stream()
                .map(VacancyDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VacancyDTO upsertVacancy(String career, Integer year, Integer limitCount) {
        int y = (year != null ? year : Year.now().getValue());
        Vacancy v = vacancyRepo.findByCareerAndAdmissionYear(career, y)
                .orElseGet(() -> {
                    Vacancy n = new Vacancy();
                    n.setCareer(career);
                    n.setAdmissionYear(y);
                    n.setLimitCount(limitCount != null ? limitCount : 0);
                    n.setAvailableSlots(limitCount != null ? limitCount : 0);
                    return n;
                });

        v.setLimitCount(limitCount != null ? limitCount : v.getLimitCount());
        // If creating new or limit changed, we may want to adjust availableSlots — keep
        // simple:
        if (v.getAvailableSlots() == null)
            v.setAvailableSlots(v.getLimitCount());
        Vacancy saved = vacancyRepo.save(v);
        return VacancyDTO.fromEntity(saved);
    }

    @Override
    @Transactional
    public List<VacancyDTO> recalculateAll(Integer year) {
        int y = (year != null ? year : Year.now().getValue());
        List<Vacancy> vacs = vacancyRepo.findByAdmissionYear(y);
        for (Vacancy v : vacs) {
            actualizarContadores(v, y);
        }
        return vacs.stream().map(VacancyDTO::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VacancyDTO recalculateOne(String career, Integer year) {
        int y = (year != null ? year : Year.now().getValue());
        Vacancy v = vacancyRepo.findByCareerAndAdmissionYear(career, y)
                .orElseThrow(() -> new RuntimeException("Vacante no encontrada para " + career + " en " + y));
        actualizarContadores(v, y);
        return VacancyDTO.fromEntity(v);
    }

    private void actualizarContadores(Vacancy v, int year) {
        String career = v.getCareer();

        long accepted = applicantRepo.countByCareerAndAdmissionYearAndStatus(career, year, "ACEPTADO");
        long pending = requestRepo.countPendingByNewCareerAndYear(career, year);

        v.setAcceptedCount((int) accepted);
        v.setPendingCount((int) pending);
        v.setAvailableSlots(Math.max(v.getLimitCount() - (int) accepted - (int) pending, 0));

        vacancyRepo.save(v);
    }
}

/*
package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.CareerChangeRequestDTO;
import com.unsis.admunsisbackend.dto.CreateCareerChangeRequestDTO;
import com.unsis.admunsisbackend.dto.ProcessCareerChangeRequestDTO;
import com.unsis.admunsisbackend.model.Applicant;
import com.unsis.admunsisbackend.model.CareerChangeRequest;
import com.unsis.admunsisbackend.model.User;
import com.unsis.admunsisbackend.dto.VacancyDTO;
import com.unsis.admunsisbackend.model.Vacancy;
import com.unsis.admunsisbackend.repository.ApplicantRepository;
import com.unsis.admunsisbackend.repository.CareerChangeRequestRepository;
import com.unsis.admunsisbackend.repository.UserRepository;
import com.unsis.admunsisbackend.repository.VacancyRepository;
import com.unsis.admunsisbackend.service.VacancyService;
import com.unsis.admunsisbackend.service.CareerChangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VacancyServiceImpl implements VacancyService {
    
    @Autowired private VacancyRepository vacancyRepo;
    @Autowired private ApplicantRepository applicantRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private CareerChangeRequestRepository requestRepo;


    @Override
    @Transactional
    public CareerChangeRequestDTO submitChange(String username, CreateCareerChangeRequestDTO dto) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Applicant app = user.getApplicant();
        if (app == null)
            throw new RuntimeException("El usuario no es un aspirante");

        String nuevaCarrera = dto.getNewCareer().trim();
        if ("LICENCIATURA EN MEDICINA".equalsIgnoreCase(nuevaCarrera)) {
            throw new RuntimeException("No está permitido postularse a Medicina");
        }

        // 1) Verificar que NO haya solicitado antes (única vez en la vida)
        boolean yaSolicito = reqRepo.findByApplicant(app).stream().findAny().isPresent();
        if (yaSolicito) {
            throw new RuntimeException("Ya realizaste una solicitud de cambio de carrera previamente");
        }

        int year = app.getAdmissionYear();

        // 2) Validar vacantes destino y reservar 1 slot (si hay availableSlots > 0)
        Vacancy vac = vacancyRepo.findByCareerAndAdmissionYear(nuevaCarrera, year)
                .orElseThrow(
                        () -> new RuntimeException("Vacantes no configuradas para " + nuevaCarrera + " en " + year));

        if (vac.getAvailableSlots() <= 0) {
            throw new RuntimeException("Cupo agotado para " + nuevaCarrera);
        }

        // reservar (decrementa availableSlots y aumenta pending_count)
        vac.setAvailableSlots(vac.getAvailableSlots() - 1);
        vac.setPendingCount(vac.getPendingCount() + 1);
        vacancyRepo.save(vac);

        // 3) Crear solicitud
        CareerChangeRequest solicitud = new CareerChangeRequest();
        solicitud.setApplicant(app);
        solicitud.setOldCareer(app.getCareer());
        solicitud.setNewCareer(nuevaCarrera);
        solicitud.setRequestComment(dto.getRequestComment());
        solicitud.setStatus("PENDIENTE"); // español
        solicitud = reqRepo.save(solicitud);

        // 4) Marcar al aspirante como "Solicitud en proceso"
        app.setStatus("Solicitud en proceso");
        applicantRepo.save(app);

        return toDto(solicitud);
    }

    /**
     * Listar solicitudes PENDIENTES (para admin / secretaría)
     *
    @Override
    public List<CareerChangeRequestDTO> listPending() {
        return reqRepo.findByStatus("PENDIENTE")
                .stream().map(this::toDto).collect(Collectors.toList());
    }
    /**
     * Procesar la solicitud: "ACEPTADO" o "RECHAZADO"
     * Si es ACEPTADO: applicant.career cambia, status -> "ACEPTADO", vacancy.accepted_count++
     * Si es RECHAZADO: applicant.status -> "RECHAZADO", libera la vacante (availableSlots++).
     *
    @Override
    @Transactional
    public CareerChangeRequestDTO processRequest(Long requestId, ProcessCareerChangeRequestDTO dto, String adminUsername) {
        CareerChangeRequest solicitud = reqRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        if (!"PENDIENTE".equalsIgnoreCase(solicitud.getStatus())) {
            throw new RuntimeException("Solicitud ya procesada");
        }

        User admin = userRepo.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Usuario que procesa no encontrado"));

        Applicant app = solicitud.getApplicant();
        String nuevaCarrera = solicitud.getNewCareer();
        int year = app.getAdmissionYear();

        // obtener vacante destino (debe existir)
        Vacancy vacDestino = vacancyRepo.findByCareerAndAdmissionYear(nuevaCarrera, year)
                .orElseThrow(() -> new RuntimeException("Vacantes no configuradas para " + nuevaCarrera));

        solicitud.setResponseComment(dto.getResponseComment());
        solicitud.setProcessedAt(LocalDateTime.now());
        solicitud.setProcessedBy(admin);

        String accion = dto.getAction() != null ? dto.getAction().trim().toUpperCase() : "";

        if ("ACEPTADO".equalsIgnoreCase(accion)) {
            // Aprobado: actualiza aspirante y contadores
            solicitud.setStatus("ACEPTADO");
            app.setCareer(nuevaCarrera);
            app.setStatus("ACEPTADO");
            applicantRepo.save(app);

            // Ya se reservó el slot en submit -> ahora contabilizamos como aceptado
            vacDestino.setAcceptedCount(vacDestino.getAcceptedCount() + 1);
            vacDestino.setPendingCount(Math.max(vacDestino.getPendingCount() - 1, 0));
            // availableSlots ya fue decrementado en submit (no tocar aquí)
            vacancyRepo.save(vacDestino);

        } else {
            // Rechazado: liberar reserva y actualizar status
            solicitud.setStatus("RECHAZADO");
            app.setStatus("RECHAZADO");
            applicantRepo.save(app);

            // Liberar el slot reservado (devolver availableSlots) y disminuir pending_count
            vacDestino.setAvailableSlots(vacDestino.getAvailableSlots() + 1);
            vacDestino.setPendingCount(Math.max(vacDestino.getPendingCount() - 1, 0));
            vacancyRepo.save(vacDestino);
        }

        solicitud = reqRepo.save(solicitud);
        return toDto(solicitud);
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

} Segunda ediccion
*/


/*
    @Override
    public List<VacancyDTO> listVacancies(Integer year) {
        int y = (year != null ? year : Year.now().getValue());
        return vacRepo.findByAdmissionYear(y)
                    .stream().map(VacancyDTO::fromEntity)
                    .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VacancyDTO upsertVacancy(String career, Integer year, Integer limitCount) {
        int y = (year != null ? year : Year.now().getValue());
        Vacancy v = vacRepo.findByCareerAndAdmissionYear(career, y)
            .orElseGet(() -> {
                Vacancy n = new Vacancy();
                n.setCareer(career);
                n.setAdmissionYear(y);
                return n;
            });

        v.setLimitCount(limitCount);
        v.setAvailableSlots(limitCount);
        Vacancy saved = vacRepo.save(v);
        
        return VacancyDTO.fromEntity(saved);
    }

    /**
     * Recalcula counters para todas las vacantes del año (si year == null usa Year.now()).
    @Transactional
    public List<VacancyDTO> recalculateAll(Integer year) {
        int y = (year != null ? year : Year.now().getValue());
        List<Vacancy> vacs = vacRepo.findByAdmissionYear(y);

        for (Vacancy v : vacs) {
            for (Vacancy v : vacs) {
                actualizarContadores(v, y);
            }
            return vacs.stream()
                    .map(VacancyDTO::fromEntity)
                    .collect(Collectors.toList());
        }

/*
            String career = v.getCareer();
            

            long accepted = applicantRepo.countByCareerAndAdmissionYearAndStatus(career, y, "ACEPTADO");
            long pendingRequests = requestRepo.countPendingByNewCareerAndYear(career, y);

            int acceptedCount = (int) accepted;
            int pendingCount = (int) pendingRequests;
            int available = Math.max(v.getLimitCount() - acceptedCount - pendingCount, 0);

//            v.setAcceptedCount(acceptedCount);
            v.setPendingCount(pendingCount);
            v.setAvailableSlots(available);
            v.setA

            vacancyRepo.save(v);
        }

        return vacs.stream().map(VacancyDTO::fromEntity).collect(Collectors.toList());

    /**
     * Recalcula sólo una vacante carrera+year (útil cuando sólo afectas 1 carrera)
    @Transactional
    public VacancyDTO recalculateOne(String career, Integer year) {
        int y = (year != null ? year : Year.now().getValue());
        Vacancy v = vacancyRepo.findByCareerAndAdmissionYear(career, y)
                .orElseThrow(() -> new RuntimeException("Vacante no encontrada"));
                actualizarContadores(v, y);
                return VacancyDTO.fromEntity(v);
    }

    private void actualizarContadores(Vacancy v, int year) {
        String career = v.getCareer();

        long accepted = applicantRepo.countByCareerAndAdmissionYearAndStatus(career, year, "ACEPTADO");
        long pending = requestRepo.countPendingByNewCareerAndYear(career, year);

        v.setAcceptedCount((int) accepted);
        v.setPendingCount((int) pending);
        v.setAvailableSlots(Math.max(v.getLimitCount() - (int) accepted - (int) pending, 0));

        vacancyRepo.save(v);
    }
}
*/




