
package com.unsis.admunsisbackend.dto;

/* Objeto de transferencia de datos para procesar solicitudes de cambio de carrera */
public class ProcessCareerChangeRequestDTO {

    private String action; // "ACEPTADO" o "RECHAZADO"
    private String responseComment;

    // getters/setters
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResponseComment() {
        return responseComment;
    }

    public void setResponseComment(String responseComment) {
        this.responseComment = responseComment;
    }
}
