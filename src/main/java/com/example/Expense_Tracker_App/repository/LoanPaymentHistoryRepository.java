package com.example.Expense_Tracker_App.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Expense_Tracker_App.entity.LoanPaymentHistory;

public interface LoanPaymentHistoryRepository extends JpaRepository<LoanPaymentHistory, Long> {

    List<LoanPaymentHistory> findByUsernameAndPaymentYmOrderByLoanIdAscIdDesc(String username, String paymentYm);

    Optional<LoanPaymentHistory> findTop1ByUsernameAndLoanIdAndPaymentYmOrderByIdDesc(String username, Long loanId, String paymentYm);
}
