package com.example.Expense_Tracker_App.dto;

import java.util.ArrayList;
import java.util.List;

public class ImportPreviewResponse {

    private boolean success;
    private String error;
    private List<ImportPreviewRow> rows = new ArrayList<>();

    public ImportPreviewResponse() {
    }

    public static ImportPreviewResponse ok(List<ImportPreviewRow> rows) {
        ImportPreviewResponse res = new ImportPreviewResponse();
        res.success = true;
        res.rows = rows;
        return res;
    }

    public static ImportPreviewResponse fail(String error) {
        ImportPreviewResponse res = new ImportPreviewResponse();
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

    public List<ImportPreviewRow> getRows() {
        return rows;
    }

    public void setRows(List<ImportPreviewRow> rows) {
        this.rows = rows;
    }
}
