package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.PdfResponse;
import com.unsis.admunsisbackend.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para exponer el endpoint unificado de reportes.
 */
@RestController
@RequestMapping("/api/report") // Asegúrate de que coincida con tu ruta actual
@CrossOrigin(origins = "*") // Ajusta según la seguridad de tu proyecto
public class PdfController {

    @Autowired
    private PdfService pdfService;

    /**
     * Endpoint unificado que retorna el JSON con el PDF y el Excel en Base64.
     */
    @GetMapping("/generate") // Asegúrate de que coincida con tu generatePdfEndpoint
    public ResponseEntity<PdfResponse> generateReport() {
        // Llamamos al servicio unificado que ya fabricamos
        PdfResponse response = pdfService.generateApplicantsReport();

        if (response.isSuccess()) {
            // 🔹 Retornamos el JSON completo de forma directa al frontend
            return ResponseEntity.ok(response);
        } else {
            // Si algo falló internamente, mandamos el error en el JSON con un estado 500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}