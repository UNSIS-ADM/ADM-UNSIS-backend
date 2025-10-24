package com.unsis.admunsisbackend.dto;

import lombok.Data;

@Data
public class PdfResponse {
    private boolean success;
    private String message;
    private byte[] fileBytes;
    private String fileName;
}
