package com.example.Expense_Tracker_App.dto;

import java.util.List;
import java.util.Map;

public class DashboardMonthlyResponse {

    private boolean success;
    private String error;

    private Integer year;
    private String provider;

    private List<Integer> months;
    private List<Double> incomeByMonth;
    private List<Double> expenseByMonth;

    // provider -> (month -> {income, expense})
    private Map<String, Map<Integer, MonthlyAmount>> providerBreakdown;

    public DashboardMonthlyResponse() {
    }

    public static DashboardMonthlyResponse ok(
            Integer year,
            String provider,
            List<Integer> months,
            List<Double> incomeByMonth,
            List<Double> expenseByMonth,
            Map<String, Map<Integer, MonthlyAmount>> providerBreakdown
    ) {
        DashboardMonthlyResponse res = new DashboardMonthlyResponse();
        res.success = true;
        res.year = year;
        res.provider = provider;
        res.months = months;
        res.incomeByMonth = incomeByMonth;
        res.expenseByMonth = expenseByMonth;
        res.providerBreakdown = providerBreakdown;
        return res;
    }

    public static DashboardMonthlyResponse fail(String error) {
        DashboardMonthlyResponse res = new DashboardMonthlyResponse();
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

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public List<Integer> getMonths() {
        return months;
    }

    public void setMonths(List<Integer> months) {
        this.months = months;
    }

    public List<Double> getIncomeByMonth() {
        return incomeByMonth;
    }

    public void setIncomeByMonth(List<Double> incomeByMonth) {
        this.incomeByMonth = incomeByMonth;
    }

    public List<Double> getExpenseByMonth() {
        return expenseByMonth;
    }

    public void setExpenseByMonth(List<Double> expenseByMonth) {
        this.expenseByMonth = expenseByMonth;
    }

    public Map<String, Map<Integer, MonthlyAmount>> getProviderBreakdown() {
        return providerBreakdown;
    }

    public void setProviderBreakdown(Map<String, Map<Integer, MonthlyAmount>> providerBreakdown) {
        this.providerBreakdown = providerBreakdown;
    }

    public static class MonthlyAmount {
        private Double income;
        private Double expense;

        public MonthlyAmount() {
        }

        public MonthlyAmount(Double income, Double expense) {
            this.income = income;
            this.expense = expense;
        }

        public Double getIncome() {
            return income;
        }

        public void setIncome(Double income) {
            this.income = income;
        }

        public Double getExpense() {
            return expense;
        }

        public void setExpense(Double expense) {
            this.expense = expense;
        }
    }
}
