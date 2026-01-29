package com.example.Expense_Tracker_App.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tx_fixed_expense_month",
                        columnNames = {"created_by", "fixed_expense_id", "gen_year", "gen_month"}
                ),
                @UniqueConstraint(
                        name = "uk_tx_fixed_income_month",
                        columnNames = {"created_by", "fixed_income_id", "gen_year", "gen_month"}
                )
        }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "tx_year", nullable = false)
    private Integer txYear;

    @Column(name = "tx_month", nullable = false)
    private Integer txMonth;

    @Column(name = "tx_date", nullable = false)
    private LocalDate txDate;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "tx_type", length = 50)
    private String txType;

    @Column(name = "tx_detail", length = 500)
    private String txDetail;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "post_balance", precision = 12, scale = 2)
    private BigDecimal postBalance;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "confirmed", length = 1)
    private String confirmed;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmed_by", length = 50)
    private String confirmedBy;

    @Column(name = "fixed_expense_id")
    private Long fixedExpenseId;

    @Column(name = "fixed_income_id")
    private Long fixedIncomeId;

    @Column(name = "gen_year")
    private Integer genYear;

    @Column(name = "gen_month")
    private Integer genMonth;

    public Transaction() {
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public Integer getTxYear() {
        return txYear;
    }

    public void setTxYear(Integer txYear) {
        this.txYear = txYear;
    }

    public Integer getTxMonth() {
        return txMonth;
    }

    public void setTxMonth(Integer txMonth) {
        this.txMonth = txMonth;
    }

    public LocalDate getTxDate() {
        return txDate;
    }

    public void setTxDate(LocalDate txDate) {
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPostBalance() {
        return postBalance;
    }

    public void setPostBalance(BigDecimal postBalance) {
        this.postBalance = postBalance;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(String confirmed) {
        this.confirmed = confirmed;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(String confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public Long getFixedExpenseId() {
        return fixedExpenseId;
    }

    public void setFixedExpenseId(Long fixedExpenseId) {
        this.fixedExpenseId = fixedExpenseId;
    }

    public Long getFixedIncomeId() {
        return fixedIncomeId;
    }

    public void setFixedIncomeId(Long fixedIncomeId) {
        this.fixedIncomeId = fixedIncomeId;
    }

    public Integer getGenYear() {
        return genYear;
    }

    public void setGenYear(Integer genYear) {
        this.genYear = genYear;
    }

    public Integer getGenMonth() {
        return genMonth;
    }

    public void setGenMonth(Integer genMonth) {
        this.genMonth = genMonth;
    }
}
