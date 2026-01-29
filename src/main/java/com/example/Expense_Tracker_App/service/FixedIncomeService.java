package com.example.Expense_Tracker_App.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Expense_Tracker_App.dto.FixedIncomeRequest;
import com.example.Expense_Tracker_App.dto.FixedIncomeResponse.FixedIncomeItem;
import com.example.Expense_Tracker_App.entity.FixedIncome;
import com.example.Expense_Tracker_App.entity.Transaction;
import com.example.Expense_Tracker_App.repository.FixedIncomeRepository;
import com.example.Expense_Tracker_App.repository.TransactionRepository;

@Service
public class FixedIncomeService {

    private final FixedIncomeRepository fixedIncomeRepository;
    private final TransactionRepository transactionRepository;

    private static final String PROVIDER_FIXED_INCOME = "FIXED_INCOME";

    public FixedIncomeService(FixedIncomeRepository fixedIncomeRepository, TransactionRepository transactionRepository) {
        this.fixedIncomeRepository = fixedIncomeRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<FixedIncomeItem> getList(String username, String status) {
        String u = requireUser(username);
        List<FixedIncome> list;
        String s = normalizeStatus(status);
        if (s == null) {
            list = fixedIncomeRepository.findByUsernameOrderByPaydayAscIdAsc(u);
        } else {
            list = fixedIncomeRepository.findByUsernameAndStatusOrderByPaydayAscIdAsc(u, s);
        }
        List<FixedIncomeItem> items = new ArrayList<>();
        for (FixedIncome e : list) {
            items.add(toItem(e));
        }
        return items;
    }

    @Transactional
    public FixedIncomeItem create(String username, FixedIncomeRequest request) {
        String u = requireUser(username);
        if (request == null) {
            throw new IllegalArgumentException("요청 데이터가 비어있습니다.");
        }

        FixedIncome entity = new FixedIncome();
        applyRequest(entity, request, u, true);
        FixedIncome saved = fixedIncomeRepository.save(entity);
        return toItem(saved);
    }

    @Transactional
    public FixedIncomeItem update(String username, FixedIncomeRequest request) {
        String u = requireUser(username);
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("수정할 항목을 선택해주세요.");
        }

        boolean hasConfirmed = transactionRepository.existsByProviderAndFixedIncomeIdAndConfirmedAndCreatedBy(
                PROVIDER_FIXED_INCOME,
                request.getId(),
                "Y",
                u
        );
        if (hasConfirmed) {
            throw new IllegalArgumentException("확정된 고정수입 내역이 있어 수정할 수 없습니다. 확정 취소 후 수정해주세요.");
        }

        FixedIncome entity = fixedIncomeRepository.findByIdAndUsername(request.getId(), u)
                .orElseThrow(() -> new IllegalArgumentException("해당 항목을 찾을 수 없습니다."));
        applyRequest(entity, request, u, false);
        FixedIncome saved = fixedIncomeRepository.save(entity);

        List<Transaction> linked = transactionRepository.findByProviderAndFixedIncomeIdAndConfirmedAndCreatedByOrderByTxYearAscTxMonthAscTxDateAscIdAsc(
                PROVIDER_FIXED_INCOME,
                saved.getId(),
                "N",
                u
        );
        for (Transaction t : linked) {
            if (t == null) continue;
            t.setDescription(saved.getTitle());
            if (saved.getAmount() != null) {
                t.setAmount(saved.getAmount());
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

        boolean hasConfirmed = transactionRepository.existsByProviderAndFixedIncomeIdAndConfirmedAndCreatedBy(
                PROVIDER_FIXED_INCOME,
                id,
                "Y",
                u
        );
        if (hasConfirmed) {
            throw new IllegalArgumentException("확정된 고정수입 내역이 있어 삭제할 수 없습니다. 확정 취소 후 삭제해주세요.");
        }

        List<Transaction> linked = transactionRepository.findByProviderAndFixedIncomeIdAndConfirmedAndCreatedByOrderByTxYearAscTxMonthAscTxDateAscIdAsc(
                PROVIDER_FIXED_INCOME,
                id,
                "N",
                u
        );
        if (linked != null && !linked.isEmpty()) {
            transactionRepository.deleteAll(linked);
        }
        return fixedIncomeRepository.deleteByIdAndUsername(id, u);
    }

    private void applyRequest(FixedIncome entity, FixedIncomeRequest request, String username, boolean isCreate) {
        String title = safeTrim(request.getTitle());
        if (title.isBlank()) {
            throw new IllegalArgumentException("항목명을 입력해주세요.");
        }
        String account = safeTrim(request.getAccount());
        Double amount = request.getAmount();
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("금액을 입력해주세요.");
        }
        Integer payday = request.getPayday();
        if (payday == null || payday < 1 || payday > 31) {
            throw new IllegalArgumentException("입금일을 선택해주세요.");
        }

        entity.setUsername(username);
        entity.setTitle(title);
        entity.setAccount(account);
        entity.setAmount(BigDecimal.valueOf(amount));
        entity.setCategory(safeTrim(request.getCategory()));
        entity.setPayday(payday);
        entity.setMemo(safeTrim(request.getMemo()));

        String type = normalizeType(request.getType());
        if (type == null) {
            type = "NORMAL";
        }
        entity.setType(type);

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

    private String normalizeType(String type) {
        String t = safeTrim(type).toUpperCase();
        if (t.isBlank() || "ALL".equals(t)) {
            return null;
        }
        if (!"NORMAL".equals(t) && !"EXTRA".equals(t)) {
            throw new IllegalArgumentException("구분값이 올바르지 않습니다.");
        }
        return t;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private FixedIncomeItem toItem(FixedIncome e) {
        FixedIncomeItem item = new FixedIncomeItem();
        item.setId(e.getId());
        item.setTitle(e.getTitle());
        item.setAccount(e.getAccount());
        item.setAmount(e.getAmount() == null ? null : e.getAmount().doubleValue());
        item.setCategory(e.getCategory());
        item.setPayday(e.getPayday());
        item.setMemo(e.getMemo());
        item.setStatus(e.getStatus());
        item.setType(e.getType());
        item.setCreatedAt(e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
        item.setUpdatedAt(e.getUpdatedAt() == null ? null : e.getUpdatedAt().toString());
        return item;
    }
}
