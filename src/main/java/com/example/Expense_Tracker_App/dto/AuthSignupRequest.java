package com.example.Expense_Tracker_App.dto;

public class AuthSignupRequest {

    private String username;
    private String password;

    public AuthSignupRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
