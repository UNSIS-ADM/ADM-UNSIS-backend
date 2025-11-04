package com.unsis.admunsisbackend.dto;

/* Objeto de transferencia de datos para crear restricciones de acceso */
public class CreateRestrictionDTO {
    private String roleName;
    private int startDay;
    private String startTime;
    private int endDay;
    private String endTime;
    private boolean enabled = true;
    private String description;

    // getters / setters
    public String getRoleName() {
        return roleName;
    }
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    public int getStartDay() {
        return startDay;
    }
    public void setStartDay(int startDay) {
        this.startDay = startDay;
    }
    public String getStartTime() {
        return startTime;
    }
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    public int getEndDay() {
        return endDay;
    }
    public void setEndDay(int endDay) {
        this.endDay = endDay;
    }
    public String getEndTime() {
        return endTime;
    }
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

}