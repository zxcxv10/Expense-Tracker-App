package com.example.Expense_Tracker_App.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "loan_payment_history",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lph_user_loan_ym", columnNames = {"username", "loan_id", "payment_ym"})
        }
)
public class LoanPaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "payment_ym", nullable = false, length = 7)
    private String paymentYm;

    @Column(name = "payment_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "remaining_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingAfter;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public BigDecimal getRemainingAfter() {
        return remainingAfter;
    }

    public void setRemainingAfter(BigDecimal remainingAfter) {
        this.remainingAfter = remainingAfter;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
