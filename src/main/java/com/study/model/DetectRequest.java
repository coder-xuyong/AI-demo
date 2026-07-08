package com.study.model;


public class DetectRequest {
    private String userQuestion;
    private String systemReply;
    private String knowledgeBase;
    // getters/setters

    public String getUserQuestion() {
        return userQuestion;
    }

    public void setUserQuestion(String userQuestion) {
        this.userQuestion = userQuestion;
    }

    public String getSystemReply() {
        return systemReply;
    }

    public void setSystemReply(String systemReply) {
        this.systemReply = systemReply;
    }

    public String getKnowledgeBase() {
        return knowledgeBase;
    }

    public void setKnowledgeBase(String knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }
}