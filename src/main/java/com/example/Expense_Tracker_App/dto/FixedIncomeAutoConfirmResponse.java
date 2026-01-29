package com.example.Expense_Tracker_App.dto;

public class FixedIncomeAutoConfirmResponse {

    private boolean success;
    private String error;

    private Integer year;
    private Integer month;
    private Integer confirmedCount;
    private String message;

    public FixedIncomeAutoConfirmResponse() {
    }

    public static FixedIncomeAutoConfirmResponse ok(Integer year, Integer month, int confirmedCount, String message) {
        FixedIncomeAutoConfirmResponse r = new FixedIncomeAutoConfirmResponse();
        r.success = true;
        r.year = year;
        r.month = month;
        r.confirmedCount = confirmedCount;
        r.message = message;
        return r;
    }

    public static FixedIncomeAutoConfirmResponse fail(String error) {
        FixedIncomeAutoConfirmResponse r = new FixedIncomeAutoConfirmResponse();
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
