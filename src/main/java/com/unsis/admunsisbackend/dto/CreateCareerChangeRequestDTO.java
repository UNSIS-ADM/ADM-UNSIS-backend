
package com.unsis.admunsisbackend.dto;

public class CreateCareerChangeRequestDTO {

    private String newCareer;
    private String requestComment;
    // getters/setters

    public String getNewCareer() {
        return newCareer;
    }

    public void setNewCareer(String newCareer) {
        this.newCareer = newCareer;
    }

    public String getRequestComment() {
        return requestComment;
    }

    public void setRequestComment(String requestComment) {
        this.requestComment = requestComment;
    }
}
