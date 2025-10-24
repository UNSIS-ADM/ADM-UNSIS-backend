package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.ContentDTO;
import com.unsis.admunsisbackend.dto.ContentPartDTO;
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

    @GetMapping("/{key}")
    public ResponseEntity<ContentDTO> getByKey(@PathVariable String key) {
        return ResponseEntity.ok(service.getByKey(key));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ContentDTO>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @PutMapping("/{key}/parts")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ContentDTO> upsertParts(@PathVariable String key, @RequestBody List<ContentPartDTO> parts) {
        return ResponseEntity.ok(service.upsertParts(key, parts));
    }

    @PutMapping("/{key}/parts/{partKey}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ContentPartDTO> upsertPart(@PathVariable String key, @PathVariable String partKey, @RequestBody ContentPartDTO dto) {
        return ResponseEntity.ok(service.upsertPart(key, partKey, dto));
    }
}