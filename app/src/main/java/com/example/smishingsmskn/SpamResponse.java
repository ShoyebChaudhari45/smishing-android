package com.example.smishingsmskn;

public class SpamResponse {
    private boolean spam;
    private String reason;

    public boolean isSpam() {
        return spam;
    }

    public String getReason() {
        return reason != null ? reason : "No reason provided";
    }
}
