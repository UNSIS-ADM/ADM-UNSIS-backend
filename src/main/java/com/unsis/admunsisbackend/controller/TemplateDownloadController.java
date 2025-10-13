package com.unsis.admunsisbackend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/templates")
@CrossOrigin(origins = { "http://localhost:4200" }, exposedHeaders = {
        "Content-Disposition", "Content-Length", "Content-Type" })
public class TemplateDownloadController {
    private static final Logger logger = LoggerFactory.getLogger(TemplateDownloadController.class);

    private static final Map<String, String> AVAILABLE = Map.of(
        "aspirantes", "templates_files/Datos de aspirantes.xlsx",
        "resultados", "templates_files/Resultados de admisión.xlsx");

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    @GetMapping("/{key}/download")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public ResponseEntity<ByteArrayResource> downloadTemplate(@PathVariable("key") String key) {
        String path = AVAILABLE.get(key);
        if (path == null) {
            logger.warn("Template key no válido: {}", key);
            return ResponseEntity.notFound().build();
        }

        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                logger.warn("Resource not found on classpath: {}", path);
                return ResponseEntity.notFound().build();
            }

            byte[] data = StreamUtils.copyToByteArray(resource.getInputStream());
            ByteArrayResource bar = new ByteArrayResource(data);
            String filename = resource.getFilename();

            // Content-Disposition con encoding UTF-8 (Spring lo genera bien con
            // filename(..., Charset))
            ContentDisposition cd = ContentDisposition.attachment()
                    .filename(filename, StandardCharsets.UTF_8)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(XLSX_MEDIA_TYPE);
            headers.setContentDisposition(cd);
            headers.setContentLength(data.length);
            headers.setCacheControl(CacheControl.noCache());

            logger.info("Sirviendo template '{}' ({} bytes)", filename, data.length);
            return ResponseEntity.ok().headers(headers).body(bar);
        } catch (Exception e) {
            logger.error("Error al servir template {}: {}", key, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public ResponseEntity<?> listTemplates() {
        return ResponseEntity.ok(AVAILABLE.keySet());
    }
}
