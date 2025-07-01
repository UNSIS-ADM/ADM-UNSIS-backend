package com.unsis.admunsisbackend.dto;

import java.util.List;

public class ExcelUploadResponse {
    private boolean success;
    private String message;
    private List<ExcelError> errors;

    public static class ExcelError {
        private int row;
        private String error;

        public ExcelError(int row, String error) {
            this.row = row;
            this.error = error;
        }

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    // Getters y Setters
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

    public List<ExcelError> getErrors() {
        return errors;
    }

    public void setErrors(List<ExcelError> errors) {
        this.errors = errors;
    }
}
