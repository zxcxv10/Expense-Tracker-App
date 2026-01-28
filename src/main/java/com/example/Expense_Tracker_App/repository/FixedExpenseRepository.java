package com.example.Expense_Tracker_App.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Expense_Tracker_App.entity.FixedExpense;

public interface FixedExpenseRepository extends JpaRepository<FixedExpense, Long> {

    List<FixedExpense> findByUsernameOrderByBillingDayAscIdAsc(String username);

    List<FixedExpense> findByUsernameAndStatusOrderByBillingDayAscIdAsc(String username, String status);

    Optional<FixedExpense> findByIdAndUsername(Long id, String username);

    long deleteByIdAndUsername(Long id, String username);
}
