package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.PdfResponse;
import com.unsis.admunsisbackend.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @GetMapping("/generate-pdf")
    public ResponseEntity<Resource> generateZipReport() {
        try {
            PdfResponse response = pdfService.generateApplicantsReport();

            if (response == null || !response.isSuccess()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // Extraemos los bytes directamente usando los nuevos getters de Lombok
            byte[] pdf = response.getPdfBytes();
            byte[] excel = response.getExcelBytes();

            // Plan de respaldo por si tu servicio aún guarda el PDF en el campo antiguo 'fileBytes'
            if (pdf == null) {
                pdf = response.getFileBytes();
            }

            // Creamos el contenedor ZIP en memoria
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                
                // 1. Inyectar PDF si existe
                if (pdf != null) {
                    ZipEntry pdfEntry = new ZipEntry("reporte_aspirantes.pdf");
                    zos.putNextEntry(pdfEntry);
                    zos.write(pdf);
                    zos.closeEntry();
                }
                
                // 2. Inyectar Excel si existe
                if (excel != null) {
                    ZipEntry excelEntry = new ZipEntry("reporte_aspirantes.xlsx");
                    zos.putNextEntry(excelEntry);
                    zos.write(excel);
                    zos.closeEntry();
                }
            }

            byte[] zipBytes = baos.toByteArray();
            ByteArrayResource resource = new ByteArrayResource(zipBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reportes_admision.zip\"")
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .contentLength(zipBytes.length)
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}