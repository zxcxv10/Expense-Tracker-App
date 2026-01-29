package com.example.Expense_Tracker_App.dto;

public class FixedIncomeAutoGenerateResponse {

    private boolean success;
    private String error;

    private Integer year;
    private Integer month;
    private Integer createdCount;
    private Integer skippedCount;
    private String message;

    public FixedIncomeAutoGenerateResponse() {
    }

    public static FixedIncomeAutoGenerateResponse ok(Integer year, Integer month, int created, int skipped, String message) {
        FixedIncomeAutoGenerateResponse r = new FixedIncomeAutoGenerateResponse();
        r.success = true;
        r.year = year;
        r.month = month;
        r.createdCount = created;
        r.skippedCount = skipped;
        r.message = message;
        return r;
    }

    public static FixedIncomeAutoGenerateResponse fail(String error) {
        FixedIncomeAutoGenerateResponse r = new FixedIncomeAutoGenerateResponse();
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

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(Integer createdCount) {
        this.createdCount = createdCount;
    }

    public Integer getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(Integer skippedCount) {
        this.skippedCount = skippedCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
