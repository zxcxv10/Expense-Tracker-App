package com.example.Expense_Tracker_App.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Expense_Tracker_App.dto.DashboardDailyResponse;
import com.example.Expense_Tracker_App.dto.DashboardMonthlyResponse;
import com.example.Expense_Tracker_App.entity.Transaction;
import com.example.Expense_Tracker_App.repository.TransactionRepository;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;

    public DashboardService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public DashboardMonthlyResponse getMonthly(Integer year, String provider, String username) {
        if (year == null) {
            throw new IllegalArgumentException("year는 필수입니다.");
        }

        String u = username == null ? "" : username.trim();
        if (u.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String p = provider == null ? "ALL" : provider.trim().toUpperCase();
        boolean isAll = p.isBlank() || "ALL".equalsIgnoreCase(p);

        List<Transaction> list = isAll
                ? transactionRepository.findByTxYearAndConfirmedAndCreatedBy(year, "Y", u)
                : transactionRepository.findByProviderAndTxYearAndConfirmedAndCreatedBy(p, year, "Y", u);

        List<Integer> months = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            months.add(m);
        }

        double[] income = new double[13];
        double[] expense = new double[13];

        // ALL일 때만 은행별 breakdown 제공
        Map<String, Map<Integer, DashboardMonthlyResponse.MonthlyAmount>> providerBreakdown = isAll ? new LinkedHashMap<>() : null;

        for (Transaction t : list) {
            if (t == null) continue;
            Integer m = t.getTxMonth();
            if (m == null || m < 1 || m > 12) continue;

            BigDecimal amt = t.getAmount();
            if (amt == null) continue;

            double v = amt.doubleValue();
            if (v >= 0) {
                income[m] += v;
            } else {
                expense[m] += Math.abs(v);
            }

            if (isAll && providerBreakdown != null) {
                String prov = t.getProvider();
                if (prov == null || prov.isBlank()) {
                    prov = "UNKNOWN";
                } else {
                    prov = prov.trim().toUpperCase();
                }

                providerBreakdown.putIfAbsent(prov, new LinkedHashMap<>());
                Map<Integer, DashboardMonthlyResponse.MonthlyAmount> byMonth = providerBreakdown.get(prov);
                byMonth.putIfAbsent(m, new DashboardMonthlyResponse.MonthlyAmount(0.0, 0.0));
                DashboardMonthlyResponse.MonthlyAmount ma = byMonth.get(m);

                double curIncome = ma.getIncome() == null ? 0.0 : ma.getIncome();
                double curExpense = ma.getExpense() == null ? 0.0 : ma.getExpense();

                if (v >= 0) {
                    ma.setIncome(curIncome + v);
                } else {
                    ma.setExpense(curExpense + Math.abs(v));
                }
            }
        }

        List<Double> incomeByMonth = new ArrayList<>();
        List<Double> expenseByMonth = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            incomeByMonth.add(income[m]);
            expenseByMonth.add(expense[m]);
        }

        return DashboardMonthlyResponse.ok(year, isAll ? "ALL" : p, months, incomeByMonth, expenseByMonth, providerBreakdown);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCategories(Integer year, Integer month, String provider, String username) {
        if (year == null || month == null) {
            throw new IllegalArgumentException("year, month는 필수입니다.");
        }
        if (month != 0 && (month < 1 || month > 12)) {
            throw new IllegalArgumentException("month가 올바르지 않습니다.");
        }

        String p = provider == null ? "ALL" : provider.trim().toUpperCase();
        boolean isAll = p.isBlank() || "ALL".equalsIgnoreCase(p);

        String u = username == null ? "" : username.trim();
        if (u.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        List<Transaction> list;
        if (month == 0) {
            list = isAll
                    ? transactionRepository.findByTxYearAndConfirmedAndCreatedBy(year, "Y", u)
                    : transactionRepository.findByProviderAndTxYearAndConfirmedAndCreatedBy(p, year, "Y", u);
        } else {
            list = isAll
                    ? transactionRepository.findByTxYearAndTxMonthAndConfirmedAndCreatedByOrderByTxDateAscIdAsc(year, month, "Y", u)
                    : transactionRepository.findByProviderAndTxYearAndTxMonthAndConfirmedAndCreatedByOrderByTxDateAscIdAsc(p, year, month, "Y", u);
        }

        Map<String, Double> incomeByCategory = new HashMap<>();
        Map<String, Double> expenseByCategory = new HashMap<>();

        for (Transaction t : list) {
            if (t == null) continue;
            BigDecimal amt = t.getAmount();
            if (amt == null) continue;
            double v = amt.doubleValue();

            String cat = t.getCategory();
            if (cat == null || cat.isBlank()) cat = "기타";

            if (v >= 0) {
                incomeByCategory.put(cat, incomeByCategory.getOrDefault(cat, 0.0) + v);
            } else {
                expenseByCategory.put(cat, expenseByCategory.getOrDefault(cat, 0.0) + Math.abs(v));
            }
        }

        Map<String, Object> res = new HashMap<>();
        res.put("provider", isAll ? "ALL" : p);
        res.put("incomeByCategory", incomeByCategory);
        res.put("expenseByCategory", expenseByCategory);
        return res;
    }

    @Transactional(readOnly = true)
    public DashboardDailyResponse getDaily(Integer year, Integer month, String provider, String username) {
        if (year == null || month == null) {
            throw new IllegalArgumentException("year, month는 필수입니다.");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month가 올바르지 않습니다.");
        }

        String u = username == null ? "" : username.trim();
        if (u.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String p = provider == null ? "ALL" : provider.trim().toUpperCase();
        boolean isAll = p.isBlank() || "ALL".equalsIgnoreCase(p);

        List<Transaction> list = isAll
                ? transactionRepository.findByTxYearAndTxMonthAndConfirmedAndCreatedByOrderByTxDateAscIdAsc(year, month, "Y", u)
                : transactionRepository.findByProviderAndTxYearAndTxMonthAndConfirmedAndCreatedByOrderByTxDateAscIdAsc(p, year, month, "Y", u);

        YearMonth ym = YearMonth.of(year, month);
        int lastDay = ym.lengthOfMonth();
        double[] income = new double[lastDay + 1];
        double[] expense = new double[lastDay + 1];

        for (Transaction t : list) {
            if (t == null || t.getTxDate() == null) continue;
            int d = t.getTxDate().getDayOfMonth();
            if (d < 1 || d > lastDay) continue;

            BigDecimal amt = t.getAmount();
            if (amt == null) continue;
            double v = amt.doubleValue();
            if (v >= 0) {
                income[d] += v;
            } else {
                expense[d] += Math.abs(v);
            }
        }

        List<DashboardDailyResponse.DailyAmount> days = new ArrayList<>();
        for (int d = 1; d <= lastDay; d++) {
            days.add(new DashboardDailyResponse.DailyAmount(d, income[d], expense[d]));
        }
        return DashboardDailyResponse.ok(year, month, isAll ? "ALL" : p, days);
    }
}
