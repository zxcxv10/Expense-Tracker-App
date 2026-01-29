package com.example.Expense_Tracker_App.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Expense_Tracker_App.dto.FixedIncomeAutoGenerateResult;
import com.example.Expense_Tracker_App.dto.FixedIncomeAutoMonthTxItem;
import com.example.Expense_Tracker_App.entity.FixedIncome;
import com.example.Expense_Tracker_App.entity.FixedIncomeAutoSetting;
import com.example.Expense_Tracker_App.entity.Transaction;
import com.example.Expense_Tracker_App.repository.FixedIncomeAutoSettingRepository;
import com.example.Expense_Tracker_App.repository.FixedIncomeRepository;
import com.example.Expense_Tracker_App.repository.TransactionRepository;

@Service
public class FixedIncomeAutoService {

    private static final String PROVIDER_FIXED_INCOME = "FIXED_INCOME";

    private final FixedIncomeRepository fixedIncomeRepository;
    private final TransactionRepository transactionRepository;
    private final FixedIncomeAutoSettingRepository settingRepository;

    public FixedIncomeAutoService(
            FixedIncomeRepository fixedIncomeRepository,
            TransactionRepository transactionRepository,
            FixedIncomeAutoSettingRepository settingRepository
    ) {
        this.fixedIncomeRepository = fixedIncomeRepository;
        this.transactionRepository = transactionRepository;
        this.settingRepository = settingRepository;
    }

    @Transactional
    public FixedIncomeAutoSetting ensureSetting(String username) {
        String u = safeTrim(username);
        if (u.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        return settingRepository.findByUsername(u).orElseGet(() -> {
            FixedIncomeAutoSetting s = new FixedIncomeAutoSetting();
            s.setUsername(u);
            s.setEnabled(true);
            s.setLastRunAt(null);
            s.setLastRunMessage(null);
            return settingRepository.save(s);
        });
    }

    @Transactional
    public FixedIncomeAutoSetting getSetting(String username) {
        return ensureSetting(username);
    }

    @Transactional
    public FixedIncomeAutoSetting updateEnabled(String username, Boolean enabled) {
        FixedIncomeAutoSetting s = ensureSetting(username);
        s.setEnabled(enabled != null ? enabled : Boolean.TRUE);
        return settingRepository.save(s);
    }

    @Transactional
    public FixedIncomeAutoGenerateResult generateForUser(String username, Integer year, Integer month, boolean updateLastRun) {
        String u = safeTrim(username);
        if (u.isBlank()) {
            throw new IllegalArgumentException("username이 올바르지 않습니다.");
        }
        if (year == null || month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("생성할 년/월이 올바르지 않습니다.");
        }

        FixedIncomeAutoSetting setting = ensureSetting(u);
        if (updateLastRun) {
            setting.setLastRunAt(LocalDateTime.now());
            setting.setLastRunMessage("실행 중...");
            settingRepository.save(setting);
        }

        List<FixedIncome> fixedList = fixedIncomeRepository.findByUsernameAndStatusOrderByPaydayAscIdAsc(u, "ACTIVE");
        int created = 0;
        int skipped = 0;

        if (fixedList == null || fixedList.isEmpty()) {
            String msg = "활성 고정수입 항목이 없습니다.";
            if (updateLastRun) {
                setting.setLastRunAt(LocalDateTime.now());
                setting.setLastRunMessage(msg);
                settingRepository.save(setting);
            }
            return new FixedIncomeAutoGenerateResult(0, 0, msg);
        }

        for (FixedIncome fi : fixedList) {
            if (fi == null || fi.getId() == null) {
                skipped++;
                continue;
            }

            boolean exists = transactionRepository.existsByCreatedByAndFixedIncomeIdAndGenYearAndGenMonth(
                    u,
                    fi.getId(),
                    year,
                    month
            );

            if (exists) {
                skipped++;
                continue;
            }

            Transaction t = buildTransactionFromFixedIncome(u, fi, year, month);
            transactionRepository.save(t);
            created++;
        }

        String msg = String.format("%d건 생성, %d건 건너뜀", created, skipped);
        if (updateLastRun) {
            setting.setLastRunAt(LocalDateTime.now());
            setting.setLastRunMessage(msg);
            settingRepository.save(setting);
        }

        return new FixedIncomeAutoGenerateResult(created, skipped, msg);
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
                PROVIDER_FIXED_INCOME,
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
                PROVIDER_FIXED_INCOME,
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
    public List<FixedIncomeAutoMonthTxItem> listFixedTransactionsForMonth(String username, Integer year, Integer month) {
        String u = safeTrim(username);
        if (u.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (year == null || month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("조회할 년/월이 올바르지 않습니다.");
        }

        List<Transaction> list = transactionRepository.findByProviderAndTxYearAndTxMonthAndCreatedByOrderByTxDateAscIdAsc(
                PROVIDER_FIXED_INCOME,
                year,
                month,
                u
        );

        List<FixedIncomeAutoMonthTxItem> items = new ArrayList<>();
        if (list == null) return items;

        for (Transaction t : list) {
            if (t == null) continue;
            String date = t.getTxDate() == null ? null : t.getTxDate().toString();
            Double amount = t.getAmount() == null ? null : t.getAmount().doubleValue();
            items.add(new FixedIncomeAutoMonthTxItem(
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

    private Transaction buildTransactionFromFixedIncome(String username, FixedIncome fi, int year, int month) {
        int payday = fi.getPayday() == null ? 1 : fi.getPayday();
        int lastDay = LocalDate.of(year, month, 1).lengthOfMonth();
        int safeDay = Math.max(1, Math.min(payday, lastDay));

        LocalDate txDate = LocalDate.of(year, month, safeDay);

        BigDecimal amt = fi.getAmount() == null ? BigDecimal.ZERO : fi.getAmount();
        BigDecimal positive = amt.signum() < 0 ? amt.negate() : amt;

        Transaction t = new Transaction();
        t.setProvider(PROVIDER_FIXED_INCOME);
        t.setTxYear(year);
        t.setTxMonth(month);
        t.setTxDate(txDate);
        t.setDescription(fi.getTitle() == null ? "고정수입" : fi.getTitle());
        t.setAmount(positive);
        t.setCategory(fi.getCategory());

        t.setCreatedBy(username);
        t.setUpdatedBy(username);

        t.setConfirmed("N");
        t.setConfirmedAt(null);
        t.setConfirmedBy(null);

        t.setFixedIncomeId(fi.getId());
        t.setGenYear(year);
        t.setGenMonth(month);

        return t;
    }

    private String safeTrim(String v) {
        return v == null ? "" : v.trim();
    }
}
