package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.AdmissionResultDTO;
import com.unsis.admunsisbackend.dto.ExcelUploadResponse;
import com.unsis.admunsisbackend.service.AdmissionResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdmissionResultController {

    @Autowired
    private AdmissionResultService service;

    // Nuevo endpoint de carga de resultados
    @PostMapping("/upload-results")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ExcelUploadResponse> uploadResults(
        @RequestParam("file") MultipartFile file, 
        @RequestParam(value = "year", required = false) Integer admissionYear) {
        return ResponseEntity.ok(service.processResultsExcel(file, admissionYear));
    }

    // Listar resultados (admin y user)
    @GetMapping("/results") 
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public ResponseEntity<List<AdmissionResultDTO>> getResults() {
        return ResponseEntity.ok(service.getAllResults());
    }
}
