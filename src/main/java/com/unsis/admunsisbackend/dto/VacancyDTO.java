package com.unsis.admunsisbackend.dto;

import com.unsis.admunsisbackend.model.Vacancy;

public class VacancyDTO {
    private String career;
    private Integer admissionYear;
    private Integer limitCount;

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

    public static VacancyDTO fromEntity(Vacancy v) {
      VacancyDTO d = new VacancyDTO();
      d.setCareer(v.getCareer());
      d.setAdmissionYear(v.getAdmissionYear());
      d.setLimitCount(v.getLimitCount());
      return d;
    }
}
