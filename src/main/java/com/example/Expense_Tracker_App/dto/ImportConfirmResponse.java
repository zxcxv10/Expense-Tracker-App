package com.example.Expense_Tracker_App.dto;

public class ImportConfirmResponse {

    private boolean success;
    private String error;
    private int inserted;

    public ImportConfirmResponse() {
    }

    public static ImportConfirmResponse ok(int inserted) {
        ImportConfirmResponse res = new ImportConfirmResponse();
        res.success = true;
        res.inserted = inserted;
        return res;
    }

    public static ImportConfirmResponse fail(String error) {
        ImportConfirmResponse res = new ImportConfirmResponse();
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

    public int getInserted() {
        return inserted;
    }

    public void setInserted(int inserted) {
        this.inserted = inserted;
    }
}
