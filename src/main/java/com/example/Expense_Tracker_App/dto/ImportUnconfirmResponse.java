package com.example.Expense_Tracker_App.dto;

public class ImportUnconfirmResponse {

    private boolean success;
    private String error;
    private long deleted;

    public ImportUnconfirmResponse() {
    }

    public static ImportUnconfirmResponse ok(long deleted) {
        ImportUnconfirmResponse res = new ImportUnconfirmResponse();
        res.success = true;
        res.deleted = deleted;
        return res;
    }

    public static ImportUnconfirmResponse fail(String error) {
        ImportUnconfirmResponse res = new ImportUnconfirmResponse();
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

    public long getDeleted() {
        return deleted;
    }

    public void setDeleted(long deleted) {
        this.deleted = deleted;
    }
}
