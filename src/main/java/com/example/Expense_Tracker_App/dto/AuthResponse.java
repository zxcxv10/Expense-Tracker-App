package com.example.Expense_Tracker_App.dto;

public class AuthResponse {

    private boolean success;
    private String error;

    private Long userId;
    private String username;
    private String role;

    public AuthResponse() {
    }

    public static AuthResponse ok(Long userId, String username, String role) {
        AuthResponse res = new AuthResponse();
        res.success = true;
        res.userId = userId;
        res.username = username;
        res.role = role;
        return res;
    }

    public static AuthResponse fail(String error) {
        AuthResponse res = new AuthResponse();
        res.success = false;
        res.error = error;
        return res;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
