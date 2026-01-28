package com.example.Expense_Tracker_App.dto;

public class FixedExpenseAutoConfirmResponse {

    private boolean success;
    private String error;

    private Integer year;
    private Integer month;
    private Integer confirmedCount;
    private String message;

    public FixedExpenseAutoConfirmResponse() {
    }

    public static FixedExpenseAutoConfirmResponse ok(Integer year, Integer month, int confirmedCount, String message) {
        FixedExpenseAutoConfirmResponse r = new FixedExpenseAutoConfirmResponse();
        r.success = true;
        r.year = year;
        r.month = month;
        r.confirmedCount = confirmedCount;
        r.message = message;
        return r;
    }

    public static FixedExpenseAutoConfirmResponse fail(String error) {
        FixedExpenseAutoConfirmResponse r = new FixedExpenseAutoConfirmResponse();
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

    public Integer getConfirmedCount() {
        return confirmedCount;
    }

    public void setConfirmedCount(Integer confirmedCount) {
        this.confirmedCount = confirmedCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
