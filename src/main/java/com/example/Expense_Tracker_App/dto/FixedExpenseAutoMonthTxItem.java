package com.example.Expense_Tracker_App.dto;

public class FixedExpenseAutoMonthTxItem {

    private Long id;
    private String date;
    private String description;
    private String category;
    private Double amount;
    private String confirmed;

    public FixedExpenseAutoMonthTxItem() {
    }

    public FixedExpenseAutoMonthTxItem(Long id, String date, String description, String category, Double amount, String confirmed) {
        this.id = id;
        this.date = date;
        this.description = description;
        this.category = category;
        this.amount = amount;
        this.confirmed = confirmed;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(String confirmed) {
        this.confirmed = confirmed;
    }
}
