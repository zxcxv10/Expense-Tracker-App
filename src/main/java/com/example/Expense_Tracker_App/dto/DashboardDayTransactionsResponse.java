package com.example.Expense_Tracker_App.dto;

import java.util.ArrayList;
import java.util.List;

public class DashboardDayTransactionsResponse {

    private boolean success;
    private String error;

    private Integer year;
    private Integer month;
    private Integer day;
    private String provider;

    private int totalCount;
    private List<DashboardDayTxItem> items = new ArrayList<>();

    public DashboardDayTransactionsResponse() {
    }

    public static DashboardDayTransactionsResponse ok(Integer year, Integer month, Integer day, String provider, List<DashboardDayTxItem> items) {
        DashboardDayTransactionsResponse r = new DashboardDayTransactionsResponse();
        r.success = true;
        r.year = year;
        r.month = month;
        r.day = day;
        r.provider = provider;
        r.items = items == null ? new ArrayList<>() : items;
        r.totalCount = r.items.size();
        return r;
    }

    public static DashboardDayTransactionsResponse fail(String error) {
        DashboardDayTransactionsResponse r = new DashboardDayTransactionsResponse();
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

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<DashboardDayTxItem> getItems() {
        return items;
    }

    public void setItems(List<DashboardDayTxItem> items) {
        this.items = items;
    }
}
