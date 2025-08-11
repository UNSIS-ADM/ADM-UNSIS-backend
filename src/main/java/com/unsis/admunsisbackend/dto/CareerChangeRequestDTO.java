package com.unsis.admunsisbackend.dto;

import java.time.LocalDateTime;

public class CareerChangeRequestDTO {

    private Long id;
    private Long applicantId;
    private Long ficha;
    private String oldCareer;
    private String newCareer;
    private String status;
    private String requestComment;
    private String responseComment;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private String processedBy; // username

    // getters/setters ...
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(Long applicantId) {
        this.applicantId = applicantId;
    }

    public Long getFicha() {
        return ficha;
    }

    public void setFicha(Long ficha) {
        this.ficha = ficha;
    }

    public String getOldCareer() {
        return oldCareer;
    }

    public void setOldCareer(String oldCareer) {
        this.oldCareer = oldCareer;
    }

    public String getNewCareer() {
        return newCareer;
    }

    public void setNewCareer(String newCareer) {
        this.newCareer = newCareer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRequestComment() {
        return requestComment;
    }

    public void setRequestComment(String requestComment) {
        this.requestComment = requestComment;
    }

    public String getResponseComment() {
        return responseComment;
    }

    public void setResponseComment(String responseComment) {
        this.responseComment = responseComment;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

}
