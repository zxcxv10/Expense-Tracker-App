package com.example.Expense_Tracker_App.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Expense_Tracker_App.entity.FixedExpenseAutoSetting;

public interface FixedExpenseAutoSettingRepository extends JpaRepository<FixedExpenseAutoSetting, Long> {

    Optional<FixedExpenseAutoSetting> findByUsername(String username);

    List<FixedExpenseAutoSetting> findByEnabled(Boolean enabled);
}
