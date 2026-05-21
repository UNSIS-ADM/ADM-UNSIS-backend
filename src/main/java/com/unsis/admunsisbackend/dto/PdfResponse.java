package com.unsis.admunsisbackend.dto;

import lombok.Data;

/* Objeto de transferencia de datos para la respuesta de PDF y Excel */
@Data
public class PdfResponse {
    private boolean success;
    private String message;
    
    // Campos nuevos para soportar ambos reportes en paralelo
    private byte[] pdfBytes;
    private byte[] excelBytes;
    
    // Mantenemos estos por compatibilidad heredada
    private byte[] fileBytes;
    private String fileName;
}