package com.example.smishingsmskn;

public class SmsResult {
    private String message;
    private String result;

    public SmsResult(String message, String result) {
        this.message = message;
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public String getResult() {
        return result;
    }
}
