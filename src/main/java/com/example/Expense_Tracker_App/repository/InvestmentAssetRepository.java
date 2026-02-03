package com.example.Expense_Tracker_App.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Expense_Tracker_App.entity.InvestmentAsset;

public interface InvestmentAssetRepository extends JpaRepository<InvestmentAsset, Long> {

    List<InvestmentAsset> findByUsernameOrderByAccountNameAscAssetTypeAscAssetNameAscIdDesc(String username);

    Optional<InvestmentAsset> findByIdAndUsername(Long id, String username);

    long deleteByIdAndUsername(Long id, String username);
}
