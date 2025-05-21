package com.admunisis.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/secretaria")
@PreAuthorize("hasRole('SECRETARIA')")
public class SecretariaController {
    
    @GetMapping("/solicitudes-cambio")
    public ResponseEntity<?> getSolicitudesCambio() {
        // Lógica para obtener solicitudes de cambio
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/aprobar-solicitud/{id}")
    public ResponseEntity<?> aprobarSolicitud(@PathVariable Long id) {
        // Lógica para aprobar solicitud
        return ResponseEntity.ok().build();
    }
}
