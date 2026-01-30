package com.example.Expense_Tracker_App.dto;

public class AdminUserUpdateResponse {

    private boolean success;
    private String error;
    private AdminUserItem user;

    public AdminUserUpdateResponse() {
    }

    public static AdminUserUpdateResponse ok(AdminUserItem user) {
        AdminUserUpdateResponse res = new AdminUserUpdateResponse();
        res.success = true;
        res.user = user;
        return res;
    }

    public static AdminUserUpdateResponse fail(String error) {
        AdminUserUpdateResponse res = new AdminUserUpdateResponse();
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

    public AdminUserItem getUser() {
        return user;
    }

    public void setUser(AdminUserItem user) {
        this.user = user;
    }
}
