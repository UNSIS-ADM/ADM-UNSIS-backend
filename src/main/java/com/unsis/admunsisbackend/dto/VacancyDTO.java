package com.unsis.admunsisbackend.dto;

import com.unsis.admunsisbackend.model.Vacancy;

public class VacancyDTO {
    private String career;
    private Integer admissionYear;
    private Integer limitCount;
    private Integer acceptedCount;
    private Integer pendingCount;
    private Integer availableSlots;

    // getters/setters omitted
    public String getCareer() {
        return career;
    }

    public void setCareer(String career) {
        this.career = career;
    }

    public Integer getAdmissionYear() {
        return admissionYear;
    }

    public void setAdmissionYear(Integer admissionYear) {
        this.admissionYear = admissionYear;
    }

    public Integer getLimitCount() {
        return limitCount;
    }

    public void setLimitCount(Integer limitCount) {
        this.limitCount = limitCount;
    }

    public Integer getAcceptedCount() {
        return acceptedCount;
    }

    public void setAcceptedCount(Integer acceptedCount) {
        this.acceptedCount = acceptedCount;
    }

    public Integer getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Integer pendingCount) {
        this.pendingCount = pendingCount;
    }

    public Integer getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(Integer availableSlots) {
        this.availableSlots = availableSlots;
    }

    public static VacancyDTO fromEntity(Vacancy v) {
        VacancyDTO d = new VacancyDTO();
        d.setCareer(v.getCareer());
        d.setAdmissionYear(v.getAdmissionYear());
        d.setLimitCount(v.getLimitCount());
        d.setAvailableSlots(v.getAvailableSlots());
        return d;
    }
}
