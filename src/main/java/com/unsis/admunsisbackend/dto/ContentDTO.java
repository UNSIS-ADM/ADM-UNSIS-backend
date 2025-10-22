package com.unsis.admunsisbackend.dto;

public class ContentDTO {
    private Long id;
    private String keyName;
    private String title;
    private String htmlContent;
    private String language;
    private boolean active = true;
    
    // getters/setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getKeyName() {
        return keyName;
    }
    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getHtmlContent() {
        return htmlContent;
    }
    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }
    public String getLanguage() {
        return language;
    }
    public void setLanguage(String language) {
        this.language = language;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
}
