package com.example.Expense_Tracker_App.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Expense_Tracker_App.entity.FixedIncome;

public interface FixedIncomeRepository extends JpaRepository<FixedIncome, Long> {

    List<FixedIncome> findByUsernameOrderByPaydayAscIdAsc(String username);

    List<FixedIncome> findByUsernameAndStatusOrderByPaydayAscIdAsc(String username, String status);

    Optional<FixedIncome> findByIdAndUsername(Long id, String username);

    long deleteByIdAndUsername(Long id, String username);
}
