package com.example.Expense_Tracker_App.dto;

import java.util.ArrayList;
import java.util.List;

public class ImportConfirmRequest {

    private String provider;
    private String createdBy;
    private List<ImportConfirmRow> rows = new ArrayList<>();

    public ImportConfirmRequest() {
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public List<ImportConfirmRow> getRows() {
        return rows;
    }

    public void setRows(List<ImportConfirmRow> rows) {
        this.rows = rows;
    }

    public static class ImportConfirmRow {
        private String date;
        private String description;
        private String txType;
        private String txDetail;
        private Double amount;
        private Double postBalance;
        private String category;
        private List<String> errors = new ArrayList<>();

        public ImportConfirmRow() {
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
    }
}
