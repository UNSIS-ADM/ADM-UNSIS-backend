package com.admunisis.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/alumno")
@PreAuthorize("hasRole('ALUMNO')")
public class AlumnoController {
    
    @GetMapping("/informacion-examen")
    public ResponseEntity<?> getInformacionExamen() {
        // Lógica para obtener información del examen
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/estatus")
    public ResponseEntity<?> getEstatusAdmision() {
        // Lógica para obtener estatus de admisión
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/solicitar-cambio")
    public ResponseEntity<?> solicitarCambioCarrera() {
        // Lógica para solicitar cambio de carrera
        return ResponseEntity.ok().build();
    }
}
