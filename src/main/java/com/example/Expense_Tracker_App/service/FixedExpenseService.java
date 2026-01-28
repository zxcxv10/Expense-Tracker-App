package com.example.Expense_Tracker_App.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Expense_Tracker_App.dto.FixedExpenseRequest;
import com.example.Expense_Tracker_App.dto.FixedExpenseResponse.FixedExpenseItem;
import com.example.Expense_Tracker_App.entity.FixedExpense;
import com.example.Expense_Tracker_App.entity.Transaction;
import com.example.Expense_Tracker_App.repository.FixedExpenseRepository;
import com.example.Expense_Tracker_App.repository.TransactionRepository;

@Service
public class FixedExpenseService {

    private final FixedExpenseRepository fixedExpenseRepository;
    private final TransactionRepository transactionRepository;

    private static final String PROVIDER_FIXED = "FIXED";

    public FixedExpenseService(FixedExpenseRepository fixedExpenseRepository, TransactionRepository transactionRepository) {
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<FixedExpenseItem> getList(String username, String status) {
        String u = requireUser(username);
        List<FixedExpense> list;
        String s = normalizeStatus(status);
        if (s == null) {
            list = fixedExpenseRepository.findByUsernameOrderByBillingDayAscIdAsc(u);
        } else {
            list = fixedExpenseRepository.findByUsernameAndStatusOrderByBillingDayAscIdAsc(u, s);
        }
        List<FixedExpenseItem> items = new ArrayList<>();
        for (FixedExpense e : list) {
            items.add(toItem(e));
        }
        return items;
    }

    @Transactional
    public FixedExpenseItem create(String username, FixedExpenseRequest request) {
        String u = requireUser(username);
        if (request == null) {
            throw new IllegalArgumentException("요청 데이터가 비어있습니다.");
        }

        FixedExpense entity = new FixedExpense();
        applyRequest(entity, request, u, true);
        FixedExpense saved = fixedExpenseRepository.save(entity);
        return toItem(saved);
    }

    @Transactional
    public FixedExpenseItem update(String username, FixedExpenseRequest request) {
        String u = requireUser(username);
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("수정할 항목을 선택해주세요.");
        }

        boolean hasConfirmed = transactionRepository.existsByProviderAndFixedExpenseIdAndConfirmedAndCreatedBy(
                PROVIDER_FIXED,
                request.getId(),
                "Y",
                u
        );
        if (hasConfirmed) {
            throw new IllegalArgumentException("확정된 고정지출 내역이 있어 수정할 수 없습니다. 확정 취소 후 수정해주세요.");
        }

        FixedExpense entity = fixedExpenseRepository.findByIdAndUsername(request.getId(), u)
                .orElseThrow(() -> new IllegalArgumentException("해당 항목을 찾을 수 없습니다."));
        applyRequest(entity, request, u, false);
        FixedExpense saved = fixedExpenseRepository.save(entity);

        List<Transaction> linked = transactionRepository.findByProviderAndFixedExpenseIdAndConfirmedAndCreatedByOrderByTxYearAscTxMonthAscTxDateAscIdAsc(
                PROVIDER_FIXED,
                saved.getId(),
                "N",
                u
        );
        for (Transaction t : linked) {
            if (t == null) continue;
            t.setDescription(saved.getTitle());
            if (saved.getAmount() != null) {
                t.setAmount(saved.getAmount().signum() <= 0 ? saved.getAmount() : saved.getAmount().negate());
            }
            t.setCategory(saved.getCategory());
            t.setUpdatedBy(u);
        }
        transactionRepository.saveAll(linked);
        return toItem(saved);
    }

    @Transactional
    public long delete(String username, Long id) {
        String u = requireUser(username);
        if (id == null) {
            throw new IllegalArgumentException("삭제할 항목을 선택해주세요.");
        }

        boolean hasConfirmed = transactionRepository.existsByProviderAndFixedExpenseIdAndConfirmedAndCreatedBy(
                PROVIDER_FIXED,
                id,
                "Y",
                u
        );
        if (hasConfirmed) {
            throw new IllegalArgumentException("확정된 고정지출 내역이 있어 삭제할 수 없습니다. 확정 취소 후 삭제해주세요.");
        }

        List<Transaction> linked = transactionRepository.findByProviderAndFixedExpenseIdAndConfirmedAndCreatedByOrderByTxYearAscTxMonthAscTxDateAscIdAsc(
                PROVIDER_FIXED,
                id,
                "N",
                u
        );
        if (linked != null && !linked.isEmpty()) {
            transactionRepository.deleteAll(linked);
        }
        return fixedExpenseRepository.deleteByIdAndUsername(id, u);
    }

    private void applyRequest(FixedExpense entity, FixedExpenseRequest request, String username, boolean isCreate) {
        String title = safeTrim(request.getTitle());
        if (title.isBlank()) {
            throw new IllegalArgumentException("항목명을 입력해주세요.");
        }
        String account = safeTrim(request.getAccount());
        Double amount = request.getAmount();
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("금액을 입력해주세요.");
        }
        Integer billingDay = request.getBillingDay();
        if (billingDay == null || billingDay < 1 || billingDay > 31) {
            throw new IllegalArgumentException("결제일을 선택해주세요.");
        }

        entity.setUsername(username);
        entity.setTitle(title);
        entity.setAccount(account);
        entity.setAmount(BigDecimal.valueOf(amount));
        entity.setCategory(safeTrim(request.getCategory()));
        entity.setBillingDay(billingDay);
        entity.setMemo(safeTrim(request.getMemo()));

        String status = normalizeStatus(request.getStatus());
        if (status == null) {
            status = "ACTIVE";
        }
        entity.setStatus(status);

        if (isCreate && entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private String requireUser(String username) {
        String u = safeTrim(username);
        if (u.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        return u;
    }

    private String normalizeStatus(String status) {
        String s = safeTrim(status).toUpperCase();
        if (s.isBlank() || "ALL".equals(s)) {
            return null;
        }
        if (!"ACTIVE".equals(s) && !"PAUSED".equals(s)) {
            throw new IllegalArgumentException("상태값이 올바르지 않습니다.");
        }
        return s;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private FixedExpenseItem toItem(FixedExpense e) {
        FixedExpenseItem item = new FixedExpenseItem();
        item.setId(e.getId());
        item.setTitle(e.getTitle());
        item.setAccount(e.getAccount());
        item.setAmount(e.getAmount() == null ? null : e.getAmount().doubleValue());
        item.setCategory(e.getCategory());
        item.setBillingDay(e.getBillingDay());
        item.setMemo(e.getMemo());
        item.setStatus(e.getStatus());
        item.setCreatedAt(e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
        item.setUpdatedAt(e.getUpdatedAt() == null ? null : e.getUpdatedAt().toString());
        return item;
    }
}
