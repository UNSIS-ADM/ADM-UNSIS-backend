package com.unsis.admunsisbackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ApplicantProfileDTO {
    private String fullName;
    private Long ficha;
    private String career;
    private String CareerAtResult;
    private String status;
    private BigDecimal score;
    private LocalDateTime resultDate;

    // getters/setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Long getFicha() {
        return ficha;
    }

    public void setFicha(Long ficha) {
        this.ficha = ficha;
    }

    public String getCareer() {
        return career;
    }

    public void setCareer(String career) {
        this.career = career;
    }

    public String getCareerAtResult() {
        return CareerAtResult;
    }

    public void setCareerAtResult(String careerAtResult) {
        CareerAtResult = careerAtResult;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public LocalDateTime getResultDate() {
        return resultDate;
    }

    public void setResultDate(LocalDateTime resultDate) {
        this.resultDate = resultDate;
    }
}
