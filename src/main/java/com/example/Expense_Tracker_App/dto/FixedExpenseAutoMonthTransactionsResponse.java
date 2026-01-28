package com.example.Expense_Tracker_App.dto;

import java.util.ArrayList;
import java.util.List;

public class FixedExpenseAutoMonthTransactionsResponse {

    private boolean success;
    private String error;

    private Integer year;
    private Integer month;

    private int totalCount;
    private int unconfirmedCount;
    private int confirmedCount;

    private List<FixedExpenseAutoMonthTxItem> items = new ArrayList<>();

    public FixedExpenseAutoMonthTransactionsResponse() {
    }

    public static FixedExpenseAutoMonthTransactionsResponse ok(
            Integer year,
            Integer month,
            List<FixedExpenseAutoMonthTxItem> items,
            int unconfirmedCount,
            int confirmedCount
    ) {
        FixedExpenseAutoMonthTransactionsResponse r = new FixedExpenseAutoMonthTransactionsResponse();
        r.success = true;
        r.year = year;
        r.month = month;
        r.items = items == null ? new ArrayList<>() : items;
        r.unconfirmedCount = unconfirmedCount;
        r.confirmedCount = confirmedCount;
        r.totalCount = r.items.size();
        return r;
    }

    public static FixedExpenseAutoMonthTransactionsResponse fail(String error) {
        FixedExpenseAutoMonthTransactionsResponse r = new FixedExpenseAutoMonthTransactionsResponse();
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

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getUnconfirmedCount() {
        return unconfirmedCount;
    }

    public void setUnconfirmedCount(int unconfirmedCount) {
        this.unconfirmedCount = unconfirmedCount;
    }

    public int getConfirmedCount() {
        return confirmedCount;
    }

    public void setConfirmedCount(int confirmedCount) {
        this.confirmedCount = confirmedCount;
    }

    public List<FixedExpenseAutoMonthTxItem> getItems() {
        return items;
    }

    public void setItems(List<FixedExpenseAutoMonthTxItem> items) {
        this.items = items;
    }
}
