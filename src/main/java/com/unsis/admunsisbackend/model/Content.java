package com.unsis.admunsisbackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "contents")
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_name", nullable = false, unique = true, length = 200)
    private String keyName;

    private String title;

    private String language = "es";

    private boolean active = true;

    @Column(name="created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name="updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // getters / setters
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
}
