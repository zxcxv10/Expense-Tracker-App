package com.example.Expense_Tracker_App.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Expense_Tracker_App.entity.Loan;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUsernameOrderByLenderAscLoanNameAscIdDesc(String username);

    Optional<Loan> findByIdAndUsername(Long id, String username);

    long deleteByIdAndUsername(Long id, String username);
}
