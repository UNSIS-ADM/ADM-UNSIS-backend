package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.ExcelUploadResponse;
import com.unsis.admunsisbackend.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
public class ExcelController {

    @Autowired
    private ExcelService excelService;

    @PostMapping("/upload-applicants")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ExcelUploadResponse> uploadApplicants(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            ExcelUploadResponse response = new ExcelUploadResponse();
            response.setSuccess(false);
            response.setMessage("Por favor seleccione un archivo");
            return ResponseEntity.badRequest().body(response);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".xlsx")) {
            ExcelUploadResponse response = new ExcelUploadResponse();
            response.setSuccess(false);
            response.setMessage("Por favor suba un archivo Excel (.xlsx)");
            return ResponseEntity.badRequest().body(response);
        }

        ExcelUploadResponse response = excelService.processExcel(file);
        return ResponseEntity.ok(response);
    }
}
