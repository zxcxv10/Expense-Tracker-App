package com.example.Expense_Tracker_App.dto;

public class FixedIncomeAutoGenerateResult {

    private final int created;
    private final int skipped;
    private final String message;

    public FixedIncomeAutoGenerateResult(int created, int skipped, String message) {
        this.created = created;
        this.skipped = skipped;
        this.message = message;
    }

    public int getCreated() {
        return created;
    }

    public int getSkipped() {
        return skipped;
    }

    public String getMessage() {
        return message;
    }
}
