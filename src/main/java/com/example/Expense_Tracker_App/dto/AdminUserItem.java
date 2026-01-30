package com.example.Expense_Tracker_App.dto;

import java.time.LocalDateTime;

public class AdminUserItem {

    private Long id;
    private String username;
    private String role;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public AdminUserItem() {
    }

    public AdminUserItem(Long id, String username, String role, Boolean enabled, LocalDateTime createdAt, LocalDateTime lastLoginAt) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
