package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.ContentDTO;
import com.unsis.admunsisbackend.service.ContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentService service;

    public ContentController(ContentService service) { this.service = service; }

    /** GET público por key (front llamará con key) */
    @GetMapping("/{key}")
    public ResponseEntity<ContentDTO> getByKey(@PathVariable("key") String key) {
        ContentDTO dto = service.getByKey(key);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }

    /** Listar (solo ADMIN) */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<ContentDTO> listAll() {
        return service.listAll();
    }

    /** Crear/actualizar (solo ADMIN) */
    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ContentDTO> upsert(@PathVariable String key, @RequestBody ContentDTO dto) {
        ContentDTO saved = service.saveOrUpdate(key, dto);
        return ResponseEntity.ok(saved);
    }
}
