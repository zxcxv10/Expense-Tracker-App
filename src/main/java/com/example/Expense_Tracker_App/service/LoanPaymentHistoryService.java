package com.example.Expense_Tracker_App.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Expense_Tracker_App.dto.LoanPaymentResponse.LoanPaymentItem;
import com.example.Expense_Tracker_App.entity.LoanPaymentHistory;
import com.example.Expense_Tracker_App.repository.LoanPaymentHistoryRepository;

@Service
public class LoanPaymentHistoryService {

    private final LoanPaymentHistoryRepository loanPaymentHistoryRepository;

    public LoanPaymentHistoryService(LoanPaymentHistoryRepository loanPaymentHistoryRepository) {
        this.loanPaymentHistoryRepository = loanPaymentHistoryRepository;
    }

    @Transactional(readOnly = true)
    public List<LoanPaymentItem> getList(String username, String ym) {
        String u = requireUser(username);
        String paymentYm = normalizeYm(ym);
        List<LoanPaymentHistory> list = loanPaymentHistoryRepository.findByUsernameAndPaymentYmOrderByLoanIdAscIdDesc(u, paymentYm);
        List<LoanPaymentItem> items = new ArrayList<>();
        for (LoanPaymentHistory e : list) {
            items.add(toItem(e));
        }
        return items;
    }

    private LoanPaymentItem toItem(LoanPaymentHistory e) {
        LoanPaymentItem it = new LoanPaymentItem();
        it.setLoanId(e.getLoanId());
        it.setPaymentYm(e.getPaymentYm());
        it.setPaymentAmount(e.getPaymentAmount() == null ? null : e.getPaymentAmount().doubleValue());
        it.setRemainingAfter(e.getRemainingAfter() == null ? null : e.getRemainingAfter().doubleValue());
        it.setCreatedAt(e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
        return it;
    }

    private String requireUser(String username) {
        String u = username == null ? "" : username.trim();
        if (u.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        return u;
    }

    private String normalizeYm(String ym) {
        String v = ym == null ? "" : ym.trim();
        if (v.matches("\\d{4}-\\d{2}")) {
            return v;
        }
        throw new IllegalArgumentException("월(YYYY-MM)을 올바르게 입력해주세요.");
    }
}
