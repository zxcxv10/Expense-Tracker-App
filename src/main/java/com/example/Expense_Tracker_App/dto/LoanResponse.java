package com.example.Expense_Tracker_App.dto;

import java.util.List;

public class LoanResponse {

    private boolean success;
    private String error;
    private LoanItem item;
    private List<LoanItem> items;
    private long deleted;

    public LoanResponse() {
    }

    public static LoanResponse okItem(LoanItem item) {
        LoanResponse res = new LoanResponse();
        res.success = true;
        res.item = item;
        return res;
    }

    public static LoanResponse okList(List<LoanItem> items) {
        LoanResponse res = new LoanResponse();
        res.success = true;
        res.items = items;
        return res;
    }

    public static LoanResponse okDeleted(long deleted) {
        LoanResponse res = new LoanResponse();
        res.success = true;
        res.deleted = deleted;
        return res;
    }

    public static LoanResponse fail(String error) {
        LoanResponse res = new LoanResponse();
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

    public LoanItem getItem() {
        return item;
    }

    public void setItem(LoanItem item) {
        this.item = item;
    }

    public List<LoanItem> getItems() {
        return items;
    }

    public void setItems(List<LoanItem> items) {
        this.items = items;
    }

    public long getDeleted() {
        return deleted;
    }

    public void setDeleted(long deleted) {
        this.deleted = deleted;
    }

    public static class LoanItem {
        private Long id;
        private String lender;
        private String loanName;
        private String loanType;
        private Double principalAmount;
        private Double remainingPrincipal;
        private Double interestRate;
        private String repaymentType;
        private Double monthlyPayment;
        private String lastPaymentYm;
        private String maturityDate;
        private String memo;
        private String createdAt;
        private String updatedAt;

        public LoanItem() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getLender() {
            return lender;
        }

        public void setLender(String lender) {
            this.lender = lender;
        }

        public String getLoanName() {
            return loanName;
        }

        public void setLoanName(String loanName) {
            this.loanName = loanName;
        }

        public String getLoanType() {
            return loanType;
        }

        public void setLoanType(String loanType) {
            this.loanType = loanType;
        }

        public Double getPrincipalAmount() {
            return principalAmount;
        }

        public void setPrincipalAmount(Double principalAmount) {
            this.principalAmount = principalAmount;
        }

        public Double getRemainingPrincipal() {
            return remainingPrincipal;
        }

        public void setRemainingPrincipal(Double remainingPrincipal) {
            this.remainingPrincipal = remainingPrincipal;
        }

        public Double getInterestRate() {
            return interestRate;
        }

        public void setInterestRate(Double interestRate) {
            this.interestRate = interestRate;
        }

        public String getRepaymentType() {
            return repaymentType;
        }

        public void setRepaymentType(String repaymentType) {
            this.repaymentType = repaymentType;
        }

        public Double getMonthlyPayment() {
            return monthlyPayment;
        }

        public void setMonthlyPayment(Double monthlyPayment) {
            this.monthlyPayment = monthlyPayment;
        }

        public String getLastPaymentYm() {
            return lastPaymentYm;
        }

        public void setLastPaymentYm(String lastPaymentYm) {
            this.lastPaymentYm = lastPaymentYm;
        }

        public String getMaturityDate() {
            return maturityDate;
        }

        public void setMaturityDate(String maturityDate) {
            this.maturityDate = maturityDate;
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
