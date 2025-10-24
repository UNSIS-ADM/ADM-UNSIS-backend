package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.PdfResponse;
import com.unsis.admunsisbackend.service.PdfService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    /**
     * Genera un PDF con la lista de aspirantes registrados.
     * Solo accesible para administradores.
     */
    @GetMapping("/generate-pdf")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> generateApplicantsPdf() {
        PdfResponse response = pdfService.generateApplicantsReport();

        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + response.getFileName())
                .contentType(MediaType.APPLICATION_PDF)
                .body(response.getFileBytes());
    }
}
