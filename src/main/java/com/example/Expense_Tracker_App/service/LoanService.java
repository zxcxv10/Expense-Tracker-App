package com.example.Expense_Tracker_App.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Expense_Tracker_App.dto.LoanRequest;
import com.example.Expense_Tracker_App.dto.LoanResponse.LoanItem;
import com.example.Expense_Tracker_App.entity.Loan;
import com.example.Expense_Tracker_App.entity.LoanPaymentHistory;
import com.example.Expense_Tracker_App.repository.LoanPaymentHistoryRepository;
import com.example.Expense_Tracker_App.repository.LoanRepository;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanPaymentHistoryRepository loanPaymentHistoryRepository;

    public LoanService(LoanRepository loanRepository, LoanPaymentHistoryRepository loanPaymentHistoryRepository) {
        this.loanRepository = loanRepository;
        this.loanPaymentHistoryRepository = loanPaymentHistoryRepository;
    }

    @Transactional(readOnly = true)
    public List<LoanItem> getList(String username) {
        String u = requireUser(username);
        List<Loan> list = loanRepository.findByUsernameOrderByLenderAscLoanNameAscIdDesc(u);
        List<LoanItem> items = new ArrayList<>();
        for (Loan e : list) {
            items.add(toItem(e));
        }
        return items;
    }

    @Transactional
    public LoanItem create(String username, LoanRequest request) {
        String u = requireUser(username);
        if (request == null) {
            throw new IllegalArgumentException("요청 데이터가 비어있습니다.");
        }

        Loan entity = new Loan();
        applyRequest(entity, request, u, true);
        Loan saved = loanRepository.save(entity);
        return toItem(saved);
    }

    @Transactional
    public LoanItem update(String username, LoanRequest request) {
        String u = requireUser(username);
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("수정할 항목을 선택해주세요.");
        }

        Loan entity = loanRepository.findByIdAndUsername(request.getId(), u)
                .orElseThrow(() -> new IllegalArgumentException("해당 항목을 찾을 수 없습니다."));
        applyRequest(entity, request, u, false);
        Loan saved = loanRepository.save(entity);
        return toItem(saved);
    }

    @Transactional
    public long delete(String username, Long id) {
        String u = requireUser(username);
        if (id == null) {
            throw new IllegalArgumentException("삭제할 항목을 선택해주세요.");
        }
        return loanRepository.deleteByIdAndUsername(id, u);
    }

    @Transactional
    public long dedupe(String username) {
        String u = requireUser(username);
        List<Loan> list = loanRepository.findByUsernameOrderByLenderAscLoanNameAscIdDesc(u);
        Map<String, Loan> keepByKey = new HashMap<>();
        Set<Long> deleteIds = new HashSet<>();

        for (Loan e : list) {
            if (e == null || e.getId() == null) continue;
            String key = buildKey(e);
            Loan keep = keepByKey.get(key);
            if (keep == null) {
                keepByKey.put(key, e);
                continue;
            }

            // keep newer(updatedAt) else higher id
            LocalDateTime a = keep.getUpdatedAt();
            LocalDateTime b = e.getUpdatedAt();
            boolean eIsNewer = false;
            if (a == null && b != null) {
                eIsNewer = true;
            } else if (a != null && b != null && b.isAfter(a)) {
                eIsNewer = true;
            } else if (a == null && b == null && e.getId() > keep.getId()) {
                eIsNewer = true;
            }

            if (eIsNewer) {
                deleteIds.add(keep.getId());
                keepByKey.put(key, e);
            } else {
                deleteIds.add(e.getId());
            }
        }

        if (deleteIds.isEmpty()) {
            return 0;
        }
        loanRepository.deleteAllById(deleteIds);
        return deleteIds.size();
    }

    private String buildKey(Loan e) {
        String lender = e.getLender() == null ? "" : e.getLender().trim();
        String loanName = e.getLoanName() == null ? "" : e.getLoanName().trim();
        String loanType = e.getLoanType() == null ? "" : e.getLoanType().trim();
        return (lender + "|" + loanName + "|" + loanType).toLowerCase();
    }

    @Transactional
    public LoanItem payMonthly(String username, Long id, String ym) {
        String u = requireUser(username);
        if (id == null) {
            throw new IllegalArgumentException("처리할 항목을 선택해주세요.");
        }

        Loan entity = loanRepository.findByIdAndUsername(id, u)
                .orElseThrow(() -> new IllegalArgumentException("해당 항목을 찾을 수 없습니다."));

        BigDecimal monthly = entity.getMonthlyPayment();
        if (monthly == null || monthly.signum() <= 0) {
            throw new IllegalArgumentException("월 상환액이 설정되어 있어야 합니다.");
        }

        BigDecimal remaining = entity.getRemainingPrincipal();
        if (remaining == null || remaining.signum() <= 0) {
            throw new IllegalArgumentException("남은 원금이 없습니다.");
        }

        String paymentYm = normalizeYm(ym);
        boolean alreadyPaid = loanPaymentHistoryRepository
                .findTop1ByUsernameAndLoanIdAndPaymentYmOrderByIdDesc(u, entity.getId(), paymentYm)
                .isPresent();
        if (alreadyPaid) {
            throw new IllegalArgumentException("선택한 월에 상환 처리가 이미 완료되었습니다.");
        }

        BigDecimal next = remaining.subtract(monthly);
        if (next.signum() < 0) {
            next = BigDecimal.ZERO;
        }

        entity.setRemainingPrincipal(next);
        entity.setUpdatedAt(LocalDateTime.now());

        Loan saved = loanRepository.save(entity);

        LoanPaymentHistory hist = new LoanPaymentHistory();
        hist.setUsername(u);
        hist.setLoanId(saved.getId());
        hist.setPaymentYm(paymentYm);
        hist.setPaymentAmount(monthly);
        hist.setRemainingAfter(next);
        loanPaymentHistoryRepository.save(hist);

        return toItem(saved);
    }

    private String normalizeYm(String ym) {
        String v = ym == null ? "" : ym.trim();
        if (v.isBlank()) {
            return YearMonth.now().toString();
        }
        if (v.matches("\\d{4}-\\d{2}")) {
            return v;
        }
        throw new IllegalArgumentException("월(YYYY-MM)을 올바르게 입력해주세요.");
    }

    private void applyRequest(Loan entity, LoanRequest request, String username, boolean isCreate) {
        String lender = safeTrim(request.getLender());
        String loanName = safeTrim(request.getLoanName());
        if (loanName.isBlank()) {
            throw new IllegalArgumentException("대출명을 입력해주세요.");
        }

        String loanType = safeTrim(request.getLoanType());
        String repaymentType = safeTrim(request.getRepaymentType());

        Double remainingPrincipal = request.getRemainingPrincipal();
        if (remainingPrincipal == null || remainingPrincipal < 0) {
            throw new IllegalArgumentException("남은 원금을 올바르게 입력해주세요.");
        }

        Double principalAmount = request.getPrincipalAmount();
        if (principalAmount != null && principalAmount < 0) {
            throw new IllegalArgumentException("원금(대출액)을 올바르게 입력해주세요.");
        }

        Double monthlyPayment = request.getMonthlyPayment();
        if (monthlyPayment != null && monthlyPayment < 0) {
            throw new IllegalArgumentException("월 상환액을 올바르게 입력해주세요.");
        }

        Double interestRate = request.getInterestRate();
        if (interestRate != null && interestRate < 0) {
            throw new IllegalArgumentException("금리(연%)를 올바르게 입력해주세요.");
        }

        LocalDate maturityDate = null;
        String maturityRaw = safeTrim(request.getMaturityDate());
        if (!maturityRaw.isBlank()) {
            try {
                maturityDate = LocalDate.parse(maturityRaw);
            } catch (Exception e) {
                throw new IllegalArgumentException("만기일을 올바르게 입력해주세요.");
            }
        }

        entity.setUsername(username);
        entity.setLender(lender.isBlank() ? null : lender);
        entity.setLoanName(loanName);
        entity.setLoanType(loanType.isBlank() ? null : loanType);
        entity.setPrincipalAmount(principalAmount == null ? null : BigDecimal.valueOf(principalAmount));
        entity.setRemainingPrincipal(BigDecimal.valueOf(remainingPrincipal));
        entity.setInterestRate(interestRate == null ? null : BigDecimal.valueOf(interestRate));
        entity.setRepaymentType(repaymentType.isBlank() ? null : repaymentType);
        entity.setMonthlyPayment(monthlyPayment == null ? null : BigDecimal.valueOf(monthlyPayment));
        entity.setMaturityDate(maturityDate);
        entity.setMemo(safeTrim(request.getMemo()));

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

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private LoanItem toItem(Loan e) {
        LoanItem item = new LoanItem();
        item.setId(e.getId());
        item.setLender(e.getLender());
        item.setLoanName(e.getLoanName());
        item.setLoanType(e.getLoanType());
        item.setPrincipalAmount(e.getPrincipalAmount() == null ? null : e.getPrincipalAmount().doubleValue());
        item.setRemainingPrincipal(e.getRemainingPrincipal() == null ? null : e.getRemainingPrincipal().doubleValue());
        item.setInterestRate(e.getInterestRate() == null ? null : e.getInterestRate().doubleValue());
        item.setRepaymentType(e.getRepaymentType());
        item.setMonthlyPayment(e.getMonthlyPayment() == null ? null : e.getMonthlyPayment().doubleValue());
        item.setLastPaymentYm(e.getLastPaymentYm());
        item.setMaturityDate(e.getMaturityDate() == null ? null : e.getMaturityDate().toString());
        item.setMemo(e.getMemo());
        item.setCreatedAt(e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
        item.setUpdatedAt(e.getUpdatedAt() == null ? null : e.getUpdatedAt().toString());
        return item;
    }
}
