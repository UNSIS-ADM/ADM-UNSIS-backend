// src/main/java/com/unsis/admunsisbackend/controller/AccessRestrictionController.java
package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.CreateRestrictionDTO;
import com.unsis.admunsisbackend.dto.AccessRestrictionDTO;
import com.unsis.admunsisbackend.model.AccessRestriction;
import com.unsis.admunsisbackend.service.AccessRestrictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/access")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AccessRestrictionController {

    @Autowired
    private AccessRestrictionService service;

    @PostMapping("/weekly")
    public ResponseEntity<AccessRestrictionDTO> create(@RequestBody CreateRestrictionDTO dto) {
        AccessRestriction w = dtoToEntity(dto);
        AccessRestriction saved = service.create(w);
        return ResponseEntity.ok(entityToDto(saved));
    }

    @PutMapping("/weekly/{id}")
    public ResponseEntity<AccessRestrictionDTO> update(@PathVariable Long id,
            @RequestBody CreateRestrictionDTO dto) {
        AccessRestriction w = dtoToEntity(dto);
        AccessRestriction saved = service.update(id, w);
        return ResponseEntity.ok(entityToDto(saved));
    }

    @GetMapping("/weekly")
    public ResponseEntity<List<AccessRestrictionDTO>> list(@RequestParam String role) {
        return ResponseEntity.ok(
                service.listForRole(role)
                        .stream()
                        .map(this::entityToDto)
                        .collect(Collectors.toList()));
    }

    @DeleteMapping("/weekly/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private AccessRestriction dtoToEntity(CreateRestrictionDTO dto) {
        AccessRestriction w = new AccessRestriction();
        w.setRoleName(dto.getRoleName());
        w.setStartDay(dto.getStartDay());
        w.setStartTime(dto.getStartTime());
        w.setEndDay(dto.getEndDay());
        w.setEndTime(dto.getEndTime());
        w.setEnabled(dto.isEnabled());
        w.setDescription(dto.getDescription());
        return w;
    }

    private AccessRestrictionDTO entityToDto(AccessRestriction w) {
        AccessRestrictionDTO d = new AccessRestrictionDTO();
        d.setId(w.getId());
        d.setRoleName(w.getRoleName());
        d.setStartDay(w.getStartDay());
        d.setStartTime(w.getStartTime());
        d.setEndDay(w.getEndDay());
        d.setEndTime(w.getEndTime());
        d.setEnabled(w.isEnabled());
        d.setDescription(w.getDescription());
        d.setCreatedAt(w.getCreatedAt());
        d.setUpdatedAt(w.getUpdatedAt());
        return d;
    }
}
