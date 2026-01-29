package com.example.Expense_Tracker_App.dto;

import java.util.List;

public class FixedIncomeResponse {

    private boolean success;
    private String error;
    private FixedIncomeItem item;
    private List<FixedIncomeItem> items;
    private long deleted;

    public FixedIncomeResponse() {
    }

    public static FixedIncomeResponse okItem(FixedIncomeItem item) {
        FixedIncomeResponse res = new FixedIncomeResponse();
        res.success = true;
        res.item = item;
        return res;
    }

    public static FixedIncomeResponse okList(List<FixedIncomeItem> items) {
        FixedIncomeResponse res = new FixedIncomeResponse();
        res.success = true;
        res.items = items;
        return res;
    }

    public static FixedIncomeResponse okDeleted(long deleted) {
        FixedIncomeResponse res = new FixedIncomeResponse();
        res.success = true;
        res.deleted = deleted;
        return res;
    }

    public static FixedIncomeResponse fail(String error) {
        FixedIncomeResponse res = new FixedIncomeResponse();
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

    public FixedIncomeItem getItem() {
        return item;
    }

    public void setItem(FixedIncomeItem item) {
        this.item = item;
    }

    public List<FixedIncomeItem> getItems() {
        return items;
    }

    public void setItems(List<FixedIncomeItem> items) {
        this.items = items;
    }

    public long getDeleted() {
        return deleted;
    }

    public void setDeleted(long deleted) {
        this.deleted = deleted;
    }

    public static class FixedIncomeItem {
        private Long id;
        private String title;
        private String account;
        private Double amount;
        private String category;
        private Integer payday;
        private String memo;
        private String status;
        private String type;
        private String createdAt;
        private String updatedAt;

        public FixedIncomeItem() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAccount() {
            return account;
        }

        public void setAccount(String account) {
            this.account = account;
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

        public Integer getPayday() {
            return payday;
        }

        public void setPayday(Integer payday) {
            this.payday = payday;
        }

        public String getMemo() {
            return memo;
        }

        public void setMemo(String memo) {
            this.memo = memo;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
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
