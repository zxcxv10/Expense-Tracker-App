package com.example.Expense_Tracker_App.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Expense_Tracker_App.dto.ImportPreviewRow;
import com.example.Expense_Tracker_App.dto.ImportConfirmRequest;
import com.example.Expense_Tracker_App.entity.Transaction;
import com.example.Expense_Tracker_App.repository.TransactionRepository;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public int confirmImport(String provider, String username, List<ImportConfirmRequest.ImportConfirmRow> rows) {
        String p = provider == null ? "" : provider.trim().toUpperCase();
        if (p.isBlank()) {
            throw new IllegalArgumentException("은행/카드사를 선택해주세요.");
        }
        String u = username == null ? "" : username.trim();
        if (u.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("저장할 데이터가 없습니다.");
        }

        Integer targetYear = null;
        Integer targetMonth = null;

        String confirmer = u;
        LocalDateTime confirmedAt = LocalDateTime.now();

        List<Transaction> entities = new ArrayList<>();
        for (ImportConfirmRequest.ImportConfirmRow r : rows) {
            if (r == null) {
                continue;
            }
            if (r.getErrors() != null && !r.getErrors().isEmpty()) {
                continue;
            }

            String dateStr = r.getDate() == null ? "" : r.getDate().trim();
            String desc = r.getDescription() == null ? "" : r.getDescription().trim();
            Double amountDouble = r.getAmount();

            if (dateStr.isBlank() || desc.isBlank() || amountDouble == null) {
                continue;
            }

            LocalDate txDate;
            try {
                txDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                continue;
            }

            if (targetYear == null || targetMonth == null) {
                targetYear = txDate.getYear();
                targetMonth = txDate.getMonthValue();
            } else {
                // 한 번의 확정 저장은 반드시 동일 년/월 데이터만 허용
                if (!targetYear.equals(txDate.getYear()) || !targetMonth.equals(txDate.getMonthValue())) {
                    throw new IllegalArgumentException("확정 저장은 한 달(년/월) 단위로만 가능합니다. 다른 월 데이터가 섞여있습니다.");
                }
            }

            Transaction t = new Transaction();
            t.setProvider(p);
            t.setTxDate(txDate);
            t.setTxYear(txDate.getYear());
            t.setTxMonth(txDate.getMonthValue());
            t.setDescription(desc);
            t.setAmount(BigDecimal.valueOf(amountDouble));
            t.setCategory(r.getCategory());

            // 확정 저장: insert 시점에 Y로 저장
            t.setConfirmed("Y");
            t.setConfirmedAt(confirmedAt);
            t.setConfirmedBy(confirmer);

            t.setCreatedBy(u);
            t.setUpdatedBy(u);

            entities.add(t);
        }

        if (entities.isEmpty()) {
            throw new IllegalArgumentException("저장 가능한 데이터가 없습니다. (오류가 있는 행은 저장되지 않습니다.)");
        }

        if (targetYear != null && targetMonth != null
                && transactionRepository.existsByProviderAndTxYearAndTxMonthAndConfirmedAndCreatedBy(p, targetYear, targetMonth, "Y", u)) {
            throw new IllegalStateException("이미 확정된 월입니다. 확정된 월은 수정/재저장이 불가합니다.");
        }

        transactionRepository.saveAll(entities);
        return entities.size();
    }

    @Transactional(readOnly = true)
    public List<ImportPreviewRow> getConfirmedRows(String provider, Integer year, Integer month, String username) {
        String p = provider == null ? "" : provider.trim().toUpperCase();
        if (p.isBlank()) {
            throw new IllegalArgumentException("은행/카드사를 선택해주세요.");
        }
        if (year == null || month == null) {
            throw new IllegalArgumentException("조회할 년/월이 올바르지 않습니다.");
        }

        String u = username == null ? "" : username.trim();
        if (u.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        List<Transaction> list = transactionRepository.findByProviderAndTxYearAndTxMonthAndConfirmedAndCreatedByOrderByTxDateAscIdAsc(
                p,
                year,
                month,
                "Y",
                u
        );
        List<ImportPreviewRow> rows = new ArrayList<>();
        for (Transaction t : list) {
            ImportPreviewRow r = new ImportPreviewRow(
                    t.getTxDate() == null ? "" : t.getTxDate().toString(),
                    t.getDescription(),
                    t.getAmount() == null ? null : t.getAmount().doubleValue(),
                    t.getCategory()
            );
            rows.add(r);
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public boolean isConfirmed(String provider, Integer year, Integer month, String username) {
        String p = provider == null ? "" : provider.trim().toUpperCase();
        if (p.isBlank() || year == null || month == null) {
            return false;
        }
        String u = username == null ? "" : username.trim();
        if (u.isBlank()) {
            return false;
        }
        return transactionRepository.existsByProviderAndTxYearAndTxMonthAndConfirmedAndCreatedBy(p, year, month, "Y", u);
    }
}
