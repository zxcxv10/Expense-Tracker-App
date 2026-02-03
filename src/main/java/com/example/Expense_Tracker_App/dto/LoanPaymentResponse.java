package com.example.Expense_Tracker_App.dto;

import java.util.List;

public class LoanPaymentResponse {

    private boolean success;
    private String error;
    private List<LoanPaymentItem> items;

    public LoanPaymentResponse() {
    }

    public static LoanPaymentResponse okList(List<LoanPaymentItem> items) {
        LoanPaymentResponse res = new LoanPaymentResponse();
        res.success = true;
        res.items = items;
        return res;
    }

    public static LoanPaymentResponse fail(String error) {
        LoanPaymentResponse res = new LoanPaymentResponse();
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

    public List<LoanPaymentItem> getItems() {
        return items;
    }

    public void setItems(List<LoanPaymentItem> items) {
        this.items = items;
    }

    public static class LoanPaymentItem {
        private Long loanId;
        private String paymentYm;
        private Double paymentAmount;
        private Double remainingAfter;
        private String createdAt;

        public LoanPaymentItem() {
        }

        public Long getLoanId() {
            return loanId;
        }

        public void setLoanId(Long loanId) {
            this.loanId = loanId;
        }

        public String getPaymentYm() {
            return paymentYm;
        }

        public void setPaymentYm(String paymentYm) {
            this.paymentYm = paymentYm;
        }

        public Double getPaymentAmount() {
            return paymentAmount;
        }

        public void setPaymentAmount(Double paymentAmount) {
            this.paymentAmount = paymentAmount;
        }

        public Double getRemainingAfter() {
            return remainingAfter;
        }

        public void setRemainingAfter(Double remainingAfter) {
            this.remainingAfter = remainingAfter;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }
}
