package com.example.Expense_Tracker_App.dto;

import java.util.List;

public class DashboardDailyResponse {

    private boolean success;
    private String error;
    private Integer year;
    private Integer month;
    private String provider;
    private List<DailyAmount> days;

    public DashboardDailyResponse() {
    }

    public static DashboardDailyResponse ok(Integer year, Integer month, String provider, List<DailyAmount> days) {
        DashboardDailyResponse res = new DashboardDailyResponse();
        res.success = true;
        res.year = year;
        res.month = month;
        res.provider = provider;
        res.days = days;
        return res;
    }

    public static DashboardDailyResponse fail(String error) {
        DashboardDailyResponse res = new DashboardDailyResponse();
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

    public List<DailyAmount> getDays() {
        return days;
    }

    public void setDays(List<DailyAmount> days) {
        this.days = days;
    }

    public static class DailyAmount {
        private Integer day;
        private Double income;
        private Double expense;

        public DailyAmount() {
        }

        public DailyAmount(Integer day, Double income, Double expense) {
            this.day = day;
            this.income = income;
            this.expense = expense;
        }

        public Integer getDay() {
            return day;
        }

        public void setDay(Integer day) {
            this.day = day;
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
