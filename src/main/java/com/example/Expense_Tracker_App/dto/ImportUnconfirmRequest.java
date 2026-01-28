package com.example.Expense_Tracker_App.dto;

public class ImportUnconfirmRequest {

    private String provider;
    private Integer year;
    private Integer month;

    public ImportUnconfirmRequest() {
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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
}
