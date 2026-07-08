package com.study.model;


public class DetectResponse {
    private boolean hallucination;
    private HallucinationType type;
    private String reason;
    // getters/setters

    public boolean isHallucination() {
        return hallucination;
    }

    public void setHallucination(boolean hallucination) {
        this.hallucination = hallucination;
    }

    public HallucinationType getType() {
        return type;
    }

    public void setType(HallucinationType type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}