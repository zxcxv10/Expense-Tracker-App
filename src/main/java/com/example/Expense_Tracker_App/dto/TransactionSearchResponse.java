package com.example.Expense_Tracker_App.dto;

import java.util.List;

public class TransactionSearchResponse {

    private boolean success;
    private String error;

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    private List<TransactionItem> items;

    public TransactionSearchResponse() {
    }

    public static TransactionSearchResponse ok(int page, int size, long totalElements, int totalPages, List<TransactionItem> items) {
        TransactionSearchResponse res = new TransactionSearchResponse();
        res.success = true;
        res.page = page;
        res.size = size;
        res.totalElements = totalElements;
        res.totalPages = totalPages;
        res.items = items;
        return res;
    }

    public static TransactionSearchResponse fail(String error) {
        TransactionSearchResponse res = new TransactionSearchResponse();
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

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<TransactionItem> getItems() {
        return items;
    }

    public void setItems(List<TransactionItem> items) {
        this.items = items;
    }

    public static class TransactionItem {
        private Long id;
        private String provider;
        private String txDate;
        private String description;
        private String txType;
        private String txDetail;
        private String category;
        private Double amount;
        private Double postBalance;

        public TransactionItem() {
        }

        public TransactionItem(
                Long id,
                String provider,
                String txDate,
                String description,
                String txType,
                String txDetail,
                String category,
                Double amount,
                Double postBalance
        ) {
            this.id = id;
            this.provider = provider;
            this.txDate = txDate;
            this.description = description;
            this.txType = txType;
            this.txDetail = txDetail;
            this.category = category;
            this.amount = amount;
            this.postBalance = postBalance;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getTxDate() {
            return txDate;
        }

        public void setTxDate(String txDate) {
            this.txDate = txDate;
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
    }
}
