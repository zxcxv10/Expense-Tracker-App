package com.example.Expense_Tracker_App.dto;

import java.util.ArrayList;
import java.util.List;

public class ImportPreviewRow {

    private String date;
    private String description;
    private Double amount;
    private String category;
    private List<String> errors = new ArrayList<>();

    public ImportPreviewRow() {
    }

    public ImportPreviewRow(String date, String description, Double amount, String category) {
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.category = category;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public void addError(String message) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(message);
    }
}
