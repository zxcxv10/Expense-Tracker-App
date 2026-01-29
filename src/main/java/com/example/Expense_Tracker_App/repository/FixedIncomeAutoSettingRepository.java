package com.example.Expense_Tracker_App.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Expense_Tracker_App.entity.FixedIncomeAutoSetting;

public interface FixedIncomeAutoSettingRepository extends JpaRepository<FixedIncomeAutoSetting, Long> {

    Optional<FixedIncomeAutoSetting> findByUsername(String username);

    List<FixedIncomeAutoSetting> findByEnabled(Boolean enabled);
}
