package com.admunisis.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        // Lógica para estadísticas del dashboard
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/cargar-excel")
    public ResponseEntity<?> cargarDatosDesdeExcel() {
        // Lógica para carga de datos desde Excel
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/aspirantes")
    public ResponseEntity<?> listarAspirantes() {
        // Lógica para listar aspirantes
        return ResponseEntity.ok().build();
    }
}
