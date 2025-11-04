package com.unsis.admunsisbackend.dto;

/* Objeto de transferencia de datos para partes de contenido */
public class ContentPartDTO {
    private Long id;
    private String partKey;
    private String title;
    private String htmlContent;
    private Integer orderIndex;

    // getters/setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPartKey() {
        return partKey;
    }

    public void setPartKey(String partKey) {
        this.partKey = partKey;
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

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}
