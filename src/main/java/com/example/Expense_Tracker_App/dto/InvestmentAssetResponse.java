package com.example.Expense_Tracker_App.dto;

import java.util.List;

public class InvestmentAssetResponse {

    private boolean success;
    private String error;
    private InvestmentAssetItem item;
    private List<InvestmentAssetItem> items;
    private long deleted;

    public InvestmentAssetResponse() {
    }

    public static InvestmentAssetResponse okItem(InvestmentAssetItem item) {
        InvestmentAssetResponse res = new InvestmentAssetResponse();
        res.success = true;
        res.item = item;
        return res;
    }

    public static InvestmentAssetResponse okList(List<InvestmentAssetItem> items) {
        InvestmentAssetResponse res = new InvestmentAssetResponse();
        res.success = true;
        res.items = items;
        return res;
    }

    public static InvestmentAssetResponse okDeleted(long deleted) {
        InvestmentAssetResponse res = new InvestmentAssetResponse();
        res.success = true;
        res.deleted = deleted;
        return res;
    }

    public static InvestmentAssetResponse fail(String error) {
        InvestmentAssetResponse res = new InvestmentAssetResponse();
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

    public InvestmentAssetItem getItem() {
        return item;
    }

    public void setItem(InvestmentAssetItem item) {
        this.item = item;
    }

    public List<InvestmentAssetItem> getItems() {
        return items;
    }

    public void setItems(List<InvestmentAssetItem> items) {
        this.items = items;
    }

    public long getDeleted() {
        return deleted;
    }

    public void setDeleted(long deleted) {
        this.deleted = deleted;
    }

    public static class InvestmentAssetItem {
        private Long id;
        private String accountName;
        private String assetType;
        private String assetName;
        private Double quantity;
        private Double avgBuyPrice;
        private Double evaluatedAmount;
        private Double costAmount;
        private String memo;
        private String createdAt;
        private String updatedAt;

        public InvestmentAssetItem() {
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

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}
