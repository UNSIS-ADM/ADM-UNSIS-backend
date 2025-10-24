package com.unsis.admunsisbackend.dto;

import java.util.List;

public class ContentDTO {
    private Long id;
    private String keyName;
    private String title;
    private String language;
    private boolean active;
    private List<ContentPartDTO> parts;
    // getters/setters
    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public String getKeyName(){return keyName;}
    public void setKeyName(String keyName){this.keyName=keyName;}
    public String getTitle(){return title;}
    public void setTitle(String title){this.title=title;}
    public String getLanguage(){return language;}
    public void setLanguage(String language){this.language=language;}
    public boolean isActive(){return active;}
    public void setActive(boolean active){this.active=active;}
    public List<ContentPartDTO> getParts(){return parts;}
    public void setParts(List<ContentPartDTO> parts){this.parts=parts;}
}
