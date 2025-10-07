package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.ApplicantResponseDTO;
import com.unsis.admunsisbackend.service.ApplicantService;
import com.unsis.admunsisbackend.service.impl.ApplicantServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/applicants") 
public class ApplicantController {
    @Autowired
    private ApplicantService service;
    


    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public ResponseEntity<List<ApplicantResponseDTO>> getAll(
        @RequestParam(value = "year", required = false) Integer year) {
        return ResponseEntity.ok(service.getAllApplicants(year));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public ResponseEntity<List<ApplicantResponseDTO>> search(
            @RequestParam(required = false) Long ficha,
            @RequestParam(required = false) String curp,
            @RequestParam(required = false) String career,
            @RequestParam(required = false, name = "fullName") String fullName) {
        return ResponseEntity.ok(
                service.searchApplicants(ficha, curp, career, fullName));
    }

    @PutMapping("/{curp}/career")
    @PreAuthorize("hasAuthority('ROLE_APPLICANT')")
    public ResponseEntity<?> changeCareerByCurp(
            @PathVariable("curp") String curp, 
            @RequestParam("career") String newCareer) {
        service.changeCareerByCurp(curp, newCareer);
        return ResponseEntity.noContent().build();
    }
    
    @Autowired
    private ApplicantService applicantService; // si tu service ahora expone markAttendance, o inyecta impl

    // POST /api/applicants/{id}/attendance
    @PostMapping("/{id}/attendance")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public ApplicantResponseDTO markAttendance(
            @PathVariable Long id,
            @RequestBody Map<String,String> body,
            Principal principal) {

        String status = body.get("status"); // "ASISTIO" o "NP"
        String username = principal != null ? principal.getName() : null;

        // Llama al service. Si tu ApplicantService interface necesita extender, llama al impl directamente
        return ((ApplicantServiceImpl) applicantService).markAttendance(id, status, username);
    }


}
