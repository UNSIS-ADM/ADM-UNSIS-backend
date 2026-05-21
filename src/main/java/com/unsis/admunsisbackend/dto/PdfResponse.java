package com.unsis.admunsisbackend.dto;

/**
 * DTO unificado para transportar los reportes en PDF y Excel hacia el Frontend.
 */
public class PdfResponse {
    private boolean success;
    private String message;
    private byte[] pdfBytes;
    private byte[] excelBytes;
    private String pdfFileName;
    private String excelFileName;

    // Constructor vacío requerido por Spring
    public PdfResponse() {
    }

    // --- GETTERS Y SETTERS TRADICIONALES ---
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public byte[] getPdfBytes() {
        return pdfBytes;
    }

    public void setPdfBytes(byte[] pdfBytes) {
        this.pdfBytes = pdfBytes;
    }

    public byte[] getExcelBytes() {
        return excelBytes;
    }

    public void setExcelBytes(byte[] excelBytes) {
        this.excelBytes = excelBytes;
    }

    public String getPdfFileName() {
        return pdfFileName;
    }

    public void setPdfFileName(String pdfFileName) {
        this.pdfFileName = pdfFileName;
    }

    public String getExcelFileName() {
        return excelFileName;
    }

    public void setExcelFileName(String excelFileName) {
        this.excelFileName = excelFileName;
    }
}