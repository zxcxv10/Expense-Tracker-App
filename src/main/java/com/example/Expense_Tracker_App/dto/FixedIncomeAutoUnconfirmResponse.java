package com.example.Expense_Tracker_App.dto;

public class FixedIncomeAutoUnconfirmResponse {

    private boolean success;
    private String error;

    private Integer year;
    private Integer month;
    private Integer unconfirmedCount;
    private String message;

    public FixedIncomeAutoUnconfirmResponse() {
    }

    public static FixedIncomeAutoUnconfirmResponse ok(Integer year, Integer month, int unconfirmedCount, String message) {
        FixedIncomeAutoUnconfirmResponse r = new FixedIncomeAutoUnconfirmResponse();
        r.success = true;
        r.year = year;
        r.month = month;
        r.unconfirmedCount = unconfirmedCount;
        r.message = message;
        return r;
    }

    public static FixedIncomeAutoUnconfirmResponse fail(String error) {
        FixedIncomeAutoUnconfirmResponse r = new FixedIncomeAutoUnconfirmResponse();
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

    public Integer getUnconfirmedCount() {
        return unconfirmedCount;
    }

    public void setUnconfirmedCount(Integer unconfirmedCount) {
        this.unconfirmedCount = unconfirmedCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
