package com.unsis.admunsisbackend.dto;

import lombok.Data;
/* Objeto de transferencia de datos para la respuesta de PDF */
@Data
public class PdfResponse {
    private boolean success;
    private String message;
    private byte[] fileBytes;
    private String fileName;
    // Getters y Setters se generan automáticamente con @Data
}
