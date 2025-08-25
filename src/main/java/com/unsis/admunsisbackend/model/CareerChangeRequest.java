package com.unsis.admunsisbackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "career_change_requests")
public class CareerChangeRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;

    @Column(name = "old_career", nullable = false)
    private String oldCareer;

    @Column(name = "old_status", nullable = false)
    private String oldStatus;

    @Column(name = "new_career", nullable = false)
    private String newCareer;

    @Column(nullable = false)
    private String status;              // PENDING, APPROVED, DENIED

    @Column(name = "request_comment")
    private String requestComment;      // Comentario del aspirante

    @Column(name = "response_comment")
    private String responseComment;     // Comentario del admin/secretaria

    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;  // Fecha de solicitud

    @Column(name = "processed_at")
    private LocalDateTime processedAt;  // Fecha de aprobación/rechazo

    @ManyToOne
    @JoinColumn(name = "processed_by")
    private User processedBy;           //Quien prcesó, aprobó/rechazó

    @PrePersist
    protected void onCreate() {
        this.requestedAt = LocalDateTime.now();
    }

    // getters / setters ...
    public Long getId() {
        return id;
    }

    public Applicant getApplicant() {
        return applicant;
    }

    public void setApplicant(Applicant applicant) {
        this.applicant = applicant;
    }

    public String getOldCareer() {
        return oldCareer;
    }

    public void setOldCareer(String oldCareer) {
        this.oldCareer = oldCareer;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
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

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public User getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(User processedBy) {
        this.processedBy = processedBy;
    }
}
