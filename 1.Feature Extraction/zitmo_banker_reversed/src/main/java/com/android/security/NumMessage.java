package com.android.security;

public class NumMessage {
    private String Message;
    private String Number;

    public NumMessage(String number, String message) {
        this.Number = number;
        this.Message = message;
    }

    public String getNumber() {
        return this.Number;
    }

    public String getMessage() {
        return this.Message;
    }
}
