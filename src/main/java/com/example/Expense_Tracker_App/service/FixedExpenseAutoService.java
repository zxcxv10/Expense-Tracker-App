package com.example.Expense_Tracker_App.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Expense_Tracker_App.dto.FixedExpenseAutoGenerateResult;
import com.example.Expense_Tracker_App.dto.FixedExpenseAutoMonthTxItem;
import com.example.Expense_Tracker_App.entity.FixedExpense;
import com.example.Expense_Tracker_App.entity.FixedExpenseAutoSetting;
import com.example.Expense_Tracker_App.entity.Transaction;
import com.example.Expense_Tracker_App.repository.FixedExpenseAutoSettingRepository;
import com.example.Expense_Tracker_App.repository.FixedExpenseRepository;
import com.example.Expense_Tracker_App.repository.TransactionRepository;

@Service
public class FixedExpenseAutoService {

    private static final String PROVIDER_FIXED = "FIXED";

    private final FixedExpenseRepository fixedExpenseRepository;
    private final TransactionRepository transactionRepository;
    private final FixedExpenseAutoSettingRepository settingRepository;

    public FixedExpenseAutoService(
            FixedExpenseRepository fixedExpenseRepository,
            TransactionRepository transactionRepository,
            FixedExpenseAutoSettingRepository settingRepository
    ) {
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.transactionRepository = transactionRepository;
        this.settingRepository = settingRepository;
    }

    @Transactional
    public FixedExpenseAutoSetting ensureSetting(String username) {
        String u = safeTrim(username);
        if (u.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        return settingRepository.findByUsername(u).orElseGet(() -> {
            FixedExpenseAutoSetting s = new FixedExpenseAutoSetting();
            s.setUsername(u);
            s.setEnabled(true);
            s.setLastRunAt(null);
            s.setLastRunMessage(null);
            return settingRepository.save(s);
        });
    }

    @Transactional
    public FixedExpenseAutoSetting getSetting(String username) {
        return ensureSetting(username);
    }

    @Transactional
    public FixedExpenseAutoSetting updateEnabled(String username, Boolean enabled) {
        FixedExpenseAutoSetting s = ensureSetting(username);
        s.setEnabled(enabled != null ? enabled : Boolean.TRUE);
        return settingRepository.save(s);
    }

    @Transactional
    public FixedExpenseAutoGenerateResult generateForUser(String username, Integer year, Integer month, boolean updateLastRun) {
        String u = safeTrim(username);
        if (u.isBlank()) {
            throw new IllegalArgumentException("username이 올바르지 않습니다.");
        }
        if (year == null || month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("생성할 년/월이 올바르지 않습니다.");
        }

        FixedExpenseAutoSetting setting = ensureSetting(u);
        if (updateLastRun) {
            setting.setLastRunAt(LocalDateTime.now());
            setting.setLastRunMessage("실행 중...");
            settingRepository.save(setting);
        }

        List<FixedExpense> fixedList = fixedExpenseRepository.findByUsernameAndStatusOrderByBillingDayAscIdAsc(u, "ACTIVE");
        int created = 0;
        int skipped = 0;

        if (fixedList == null || fixedList.isEmpty()) {
            String msg = "활성 고정지출 항목이 없습니다.";
            if (updateLastRun) {
                setting.setLastRunAt(LocalDateTime.now());
                setting.setLastRunMessage(msg);
                settingRepository.save(setting);
            }
            return new FixedExpenseAutoGenerateResult(0, 0, msg);
        }

        for (FixedExpense fe : fixedList) {
            if (fe == null || fe.getId() == null) {
                skipped++;
                continue;
            }

            boolean exists = transactionRepository.existsByCreatedByAndFixedExpenseIdAndGenYearAndGenMonth(
                    u,
                    fe.getId(),
                    year,
                    month
            );

            if (exists) {
                skipped++;
                continue;
            }

            Transaction t = buildTransactionFromFixedExpense(u, fe, year, month);
            transactionRepository.save(t);
            created++;
        }

        String msg = String.format("%d건 생성, %d건 건너뜀", created, skipped);
        if (updateLastRun) {
            setting.setLastRunAt(LocalDateTime.now());
            setting.setLastRunMessage(msg);
            settingRepository.save(setting);
        }

        return new FixedExpenseAutoGenerateResult(created, skipped, msg);
    }

    @Transactional
    public int confirmForUser(String username, Integer year, Integer month) {
        String u = safeTrim(username);
        if (u.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (year == null || month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("확정할 년/월이 올바르지 않습니다.");
        }

        List<Transaction> list = transactionRepository.findByProviderAndTxYearAndTxMonthAndConfirmedAndCreatedByOrderByTxDateAscIdAsc(
                PROVIDER_FIXED,
                year,
                month,
                "N",
                u
        );

        if (list == null || list.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Transaction t : list) {
            t.setConfirmed("Y");
            t.setConfirmedAt(now);
            t.setConfirmedBy(u);
            t.setUpdatedBy(u);
        }
        transactionRepository.saveAll(list);
        return list.size();
    }

    @Transactional
    public int unconfirmForUser(String username, Integer year, Integer month) {
        String u = safeTrim(username);
        if (u.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (year == null || month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("취소할 년/월이 올바르지 않습니다.");
        }

        List<Transaction> list = transactionRepository.findByProviderAndTxYearAndTxMonthAndConfirmedAndCreatedByOrderByTxDateAscIdAsc(
                PROVIDER_FIXED,
                year,
                month,
                "Y",
                u
        );

        if (list == null || list.isEmpty()) {
            return 0;
        }

        for (Transaction t : list) {
            t.setConfirmed("N");
            t.setConfirmedAt(null);
            t.setConfirmedBy(null);
            t.setUpdatedBy(u);
        }
        transactionRepository.saveAll(list);
        return list.size();
    }

    @Transactional(readOnly = true)
    public List<FixedExpenseAutoMonthTxItem> listFixedTransactionsForMonth(String username, Integer year, Integer month) {
        String u = safeTrim(username);
        if (u.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (year == null || month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("조회할 년/월이 올바르지 않습니다.");
        }

        List<Transaction> list = transactionRepository.findByProviderAndTxYearAndTxMonthAndCreatedByOrderByTxDateAscIdAsc(
                PROVIDER_FIXED,
                year,
                month,
                u
        );

        List<FixedExpenseAutoMonthTxItem> items = new ArrayList<>();
        if (list == null) return items;

        for (Transaction t : list) {
            if (t == null) continue;
            String date = t.getTxDate() == null ? null : t.getTxDate().toString();
            Double amount = t.getAmount() == null ? null : t.getAmount().doubleValue();
            items.add(new FixedExpenseAutoMonthTxItem(
                    t.getId(),
                    date,
                    t.getDescription(),
                    t.getCategory(),
                    amount,
                    t.getConfirmed()
            ));
        }
        return items;
    }

    private Transaction buildTransactionFromFixedExpense(String username, FixedExpense fe, int year, int month) {
        int billingDay = fe.getBillingDay() == null ? 1 : fe.getBillingDay();
        int lastDay = LocalDate.of(year, month, 1).lengthOfMonth();
        int safeDay = Math.max(1, Math.min(billingDay, lastDay));

        LocalDate txDate = LocalDate.of(year, month, safeDay);

        BigDecimal amt = fe.getAmount() == null ? BigDecimal.ZERO : fe.getAmount();
        BigDecimal negative = amt.signum() <= 0 ? amt : amt.negate();

        Transaction t = new Transaction();
        t.setProvider(PROVIDER_FIXED);
        t.setTxYear(year);
        t.setTxMonth(month);
        t.setTxDate(txDate);
        t.setDescription(fe.getTitle() == null ? "고정지출" : fe.getTitle());
        t.setAmount(negative);
        t.setCategory(fe.getCategory());

        t.setCreatedBy(username);
        t.setUpdatedBy(username);

        t.setConfirmed("N");
        t.setConfirmedAt(null);
        t.setConfirmedBy(null);

        t.setFixedExpenseId(fe.getId());
        t.setGenYear(year);
        t.setGenMonth(month);

        return t;
    }

    private String safeTrim(String v) {
        return v == null ? "" : v.trim();
    }
}
