package com.example.Expense_Tracker_App.dto;

import java.util.ArrayList;
import java.util.List;

public class ConfirmedTransactionsResponse {

    private boolean success;
    private String error;
    private boolean confirmed;
    private List<ImportPreviewRow> rows = new ArrayList<>();

    public ConfirmedTransactionsResponse() {
    }

    public static ConfirmedTransactionsResponse ok(boolean confirmed, List<ImportPreviewRow> rows) {
        ConfirmedTransactionsResponse res = new ConfirmedTransactionsResponse();
        res.success = true;
        res.confirmed = confirmed;
        res.rows = rows;
        return res;
    }

    public static ConfirmedTransactionsResponse fail(String error) {
        ConfirmedTransactionsResponse res = new ConfirmedTransactionsResponse();
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

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public List<ImportPreviewRow> getRows() {
        return rows;
    }

    public void setRows(List<ImportPreviewRow> rows) {
        this.rows = rows;
    }
}
