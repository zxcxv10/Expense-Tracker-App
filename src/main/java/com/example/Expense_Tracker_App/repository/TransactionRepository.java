package com.example.Expense_Tracker_App.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Expense_Tracker_App.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByProviderAndTxYearAndTxMonthAndConfirmed(String provider, Integer txYear, Integer txMonth, String confirmed);

    boolean existsByProviderAndTxYearAndTxMonthAndConfirmedAndCreatedBy(
            String provider,
            Integer txYear,
            Integer txMonth,
            String confirmed,
            String createdBy
    );

    List<Transaction> findByProviderAndTxYearAndTxMonthAndConfirmedOrderByTxDateAscIdAsc(
            String provider,
            Integer txYear,
            Integer txMonth,
            String confirmed
    );

    List<Transaction> findByProviderAndTxYearAndTxMonthAndConfirmedAndCreatedByOrderByTxDateAscIdAsc(
            String provider,
            Integer txYear,
            Integer txMonth,
            String confirmed,
            String createdBy
    );

    List<Transaction> findByTxYearAndTxMonthAndConfirmedOrderByTxDateAscIdAsc(
            Integer txYear,
            Integer txMonth,
            String confirmed
    );

    List<Transaction> findByTxYearAndTxMonthAndConfirmedAndCreatedByOrderByTxDateAscIdAsc(
            Integer txYear,
            Integer txMonth,
            String confirmed,
            String createdBy
    );

    List<Transaction> findByTxYearAndConfirmed(Integer txYear, String confirmed);

    List<Transaction> findByTxYearAndConfirmedAndCreatedBy(Integer txYear, String confirmed, String createdBy);

    List<Transaction> findByProviderAndTxYearAndConfirmed(String provider, Integer txYear, String confirmed);

    List<Transaction> findByProviderAndTxYearAndConfirmedAndCreatedBy(
            String provider,
            Integer txYear,
            String confirmed,
            String createdBy
    );
}
