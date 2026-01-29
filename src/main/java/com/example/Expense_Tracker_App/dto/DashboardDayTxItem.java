package com.example.Expense_Tracker_App.dto;

public class DashboardDayTxItem {

    private Long id;
    private String date;
    private String provider;
    private String description;
    private String txType;
    private String txDetail;
    private String category;
    private Double amount;
    private Double postBalance;
    private String confirmed;

    public DashboardDayTxItem() {
    }

    public DashboardDayTxItem(Long id, String date, String provider, String description, String category, Double amount, String confirmed) {
        this.id = id;
        this.date = date;
        this.provider = provider;
        this.description = description;
        this.category = category;
        this.amount = amount;
        this.confirmed = confirmed;
    }

    public DashboardDayTxItem(
            Long id,
            String date,
            String provider,
            String description,
            String txType,
            String txDetail,
            String category,
            Double amount,
            Double postBalance,
            String confirmed
    ) {
        this.id = id;
        this.date = date;
        this.provider = provider;
        this.description = description;
        this.txType = txType;
        this.txDetail = txDetail;
        this.category = category;
        this.amount = amount;
        this.postBalance = postBalance;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTxType() {
        return txType;
    }

    public void setTxType(String txType) {
        this.txType = txType;
    }

    public String getTxDetail() {
        return txDetail;
    }

    public void setTxDetail(String txDetail) {
        this.txDetail = txDetail;
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

    public Double getPostBalance() {
        return postBalance;
    }

    public void setPostBalance(Double postBalance) {
        this.postBalance = postBalance;
    }

    public String getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(String confirmed) {
        this.confirmed = confirmed;
    }
}
