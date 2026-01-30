package com.example.Expense_Tracker_App.dto;

public class AdminUserUpdateRequest {

    private String role;
    private Boolean enabled;

    public AdminUserUpdateRequest() {
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
