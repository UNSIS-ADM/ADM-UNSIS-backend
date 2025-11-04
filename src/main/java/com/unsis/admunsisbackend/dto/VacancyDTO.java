package com.unsis.admunsisbackend.dto;

import com.unsis.admunsisbackend.model.Vacancy;

/* Objeto de transferencia de datos para vacantes */
public class VacancyDTO {
    private String career;
    private Integer admissionYear;
    private Integer inscritosCount;
    private Integer cuposInserted;
    private Integer availableSlots;
    private Integer releasedCount;

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

    public Integer getInscritosCount() {
        return inscritosCount;
    }

    public void setInscritosCount(Integer inscritosCount) {
        this.inscritosCount = inscritosCount;
    }

    public Integer getCuposInserted() {
        return cuposInserted;
    }

    public void setCuposInserted(Integer cuposInserted) {
        this.cuposInserted = cuposInserted;
    }

    public Integer getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(Integer availableSlots) {
        this.availableSlots = availableSlots;
    }

    public Integer getReleasedCount() {
        return releasedCount;
    }

    public void setReleasedCount(Integer releasedCount) {
        this.releasedCount = releasedCount;
    }

    public static VacancyDTO fromEntity(Vacancy v) {
        VacancyDTO d = new VacancyDTO();
        d.setCareer(v.getCareer());
        d.setAdmissionYear(v.getAdmissionYear());
        d.setInscritosCount(v.getInscritosCount());
        d.setCuposInserted(v.getCuposInserted());
        d.setAvailableSlots(v.getAvailableSlots());
        return d;
    }

    public static VacancyDTO of(String career, Integer availableSlots) {
        VacancyDTO d = new VacancyDTO();
        d.setCareer(career);
        d.setAvailableSlots(availableSlots);
        return d;
    }
}
