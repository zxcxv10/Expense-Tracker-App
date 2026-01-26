package com.example.Expense_Tracker_App.dto;

import java.util.Map;

public class DashboardCategoryResponse {

    private boolean success;
    private String error;

    private Integer year;
    private Integer month;
    private String provider;

    private Map<String, Double> incomeByCategory;
    private Map<String, Double> expenseByCategory;

    public DashboardCategoryResponse() {
    }

    public static DashboardCategoryResponse ok(
            Integer year,
            Integer month,
            String provider,
            Map<String, Double> incomeByCategory,
            Map<String, Double> expenseByCategory
    ) {
        DashboardCategoryResponse res = new DashboardCategoryResponse();
        res.success = true;
        res.year = year;
        res.month = month;
        res.provider = provider;
        res.incomeByCategory = incomeByCategory;
        res.expenseByCategory = expenseByCategory;
        return res;
    }

    public static DashboardCategoryResponse fail(String error) {
        DashboardCategoryResponse res = new DashboardCategoryResponse();
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

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Map<String, Double> getIncomeByCategory() {
        return incomeByCategory;
    }

    public void setIncomeByCategory(Map<String, Double> incomeByCategory) {
        this.incomeByCategory = incomeByCategory;
    }

    public Map<String, Double> getExpenseByCategory() {
        return expenseByCategory;
    }

    public void setExpenseByCategory(Map<String, Double> expenseByCategory) {
        this.expenseByCategory = expenseByCategory;
    }
}
