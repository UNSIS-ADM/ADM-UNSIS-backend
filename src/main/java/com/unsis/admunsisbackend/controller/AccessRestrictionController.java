package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.AccessRestrictionDTO;
import com.unsis.admunsisbackend.model.AccessRestriction;
import com.unsis.admunsisbackend.service.AccessRestrictionService;
import com.unsis.admunsisbackend.service.impl.AccessRestrictionServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/admin/access-restriction")
public class AccessRestrictionController {

    @Autowired
    private AccessRestrictionService service;

    // Obtener la regla actual (si existe)
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AccessRestrictionDTO> get() {
        AccessRestriction r = service.getRestriction();
        return ResponseEntity.ok(AccessRestrictionServiceImpl.toDto(r));
    }

    // Crear o actualizar (solo ADMIN)
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AccessRestrictionDTO> createOrUpdate(@RequestBody AccessRestrictionDTO dto) {
        AccessRestriction e = AccessRestrictionServiceImpl.fromDto(dto);
        AccessRestriction saved = service.saveOrUpdate(e);
        return ResponseEntity.ok(AccessRestrictionServiceImpl.toDto(saved));
    }

    // Activar / desactivar (PATCH) -> actualiza enabled
    @PatchMapping("/enabled/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AccessRestrictionDTO> toggleEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        AccessRestriction r = service.getRestriction();
        if (r == null || !r.getId().equals(id)) {
            return ResponseEntity.notFound().build();
        }
        r.setEnabled(enabled);
        AccessRestriction saved = service.saveOrUpdate(r);
        return ResponseEntity.ok(AccessRestrictionServiceImpl.toDto(saved));
    }
}
