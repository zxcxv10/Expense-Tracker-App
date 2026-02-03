package com.example.Expense_Tracker_App.dto;

public class InvestmentAssetRequest {

    private Long id;
    private String accountName;
    private String assetType;
    private String assetName;
    private Double quantity;
    private Double avgBuyPrice;
    private Double evaluatedAmount;
    private Double costAmount;
    private String memo;

    public InvestmentAssetRequest() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getAvgBuyPrice() {
        return avgBuyPrice;
    }

    public void setAvgBuyPrice(Double avgBuyPrice) {
        this.avgBuyPrice = avgBuyPrice;
    }

    public Double getEvaluatedAmount() {
        return evaluatedAmount;
    }

    public void setEvaluatedAmount(Double evaluatedAmount) {
        this.evaluatedAmount = evaluatedAmount;
    }

    public Double getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(Double costAmount) {
        this.costAmount = costAmount;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
}
