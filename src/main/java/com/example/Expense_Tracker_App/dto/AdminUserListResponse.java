package com.example.Expense_Tracker_App.dto;

import java.util.ArrayList;
import java.util.List;

public class AdminUserListResponse {

    private boolean success;
    private String error;
    private List<AdminUserItem> users;

    public AdminUserListResponse() {
    }

    public static AdminUserListResponse ok(List<AdminUserItem> users) {
        AdminUserListResponse res = new AdminUserListResponse();
        res.success = true;
        res.users = (users == null) ? new ArrayList<>() : users;
        return res;
    }

    public static AdminUserListResponse fail(String error) {
        AdminUserListResponse res = new AdminUserListResponse();
        res.success = false;
        res.error = error;
        res.users = new ArrayList<>();
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

    public List<AdminUserItem> getUsers() {
        return users;
    }

    public void setUsers(List<AdminUserItem> users) {
        this.users = users;
    }
}
