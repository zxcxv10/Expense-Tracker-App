package com.example.Expense_Tracker_App.dto;

public class FixedExpenseAutoSettingResponse {

    private boolean success;
    private String error;

    private Boolean enabled;
    private String lastRunAt;
    private String lastRunMessage;

    public FixedExpenseAutoSettingResponse() {
    }

    public static FixedExpenseAutoSettingResponse ok(Boolean enabled, String lastRunAt, String lastRunMessage) {
        FixedExpenseAutoSettingResponse r = new FixedExpenseAutoSettingResponse();
        r.success = true;
        r.enabled = enabled;
        r.lastRunAt = lastRunAt;
        r.lastRunMessage = lastRunMessage;
        return r;
    }

    public static FixedExpenseAutoSettingResponse fail(String error) {
        FixedExpenseAutoSettingResponse r = new FixedExpenseAutoSettingResponse();
        r.success = false;
        r.error = error;
        return r;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(String lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public String getLastRunMessage() {
        return lastRunMessage;
    }

    public void setLastRunMessage(String lastRunMessage) {
        this.lastRunMessage = lastRunMessage;
    }
}
