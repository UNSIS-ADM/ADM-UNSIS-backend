package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.*;
import com.unsis.admunsisbackend.service.CareerChangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CareerChangeController {

  @Autowired private CareerChangeService service;

  // 1) El aspirante solicita cambio
  @PostMapping("/applicant/change-career")
  @PreAuthorize("hasAuthority('ROLE_APPLICANT')")
  public ResponseEntity<CareerChangeRequestDTO> submit(
      @AuthenticationPrincipal UserDetails ud,
      @RequestBody CreateCareerChangeRequestDTO dto) {
    return ResponseEntity.ok(
        service.submitChange(ud.getUsername(), dto));
  }

  // 2) Listar solicitudes pendientes (Admin/Secretaría)
  @GetMapping("/admin/change-career/requests")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
  public List<CareerChangeRequestDTO> list() {
    return service.listPending();
  }

  // 3) Procesar (aprobar/rechazar) una solicitud
  @PutMapping("/admin/change-career/requests/{id}")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
  public ResponseEntity<CareerChangeRequestDTO> process(
      @PathVariable Long id,
      @AuthenticationPrincipal UserDetails ud,
      @RequestBody ProcessCareerChangeRequestDTO dto) {
    return ResponseEntity.ok(
      service.processRequest(id, dto, ud.getUsername()));
  }
}
