package com.example.Expense_Tracker_App.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Expense_Tracker_App.dto.ImportPreviewRow;
import com.example.Expense_Tracker_App.dto.ImportConfirmRequest;
import com.example.Expense_Tracker_App.dto.TransactionSearchResponse;
import com.example.Expense_Tracker_App.entity.Transaction;
import com.example.Expense_Tracker_App.repository.TransactionRepository;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public TransactionSearchResponse searchConfirmedTransactions(
            String username,
            String provider,
            String startDate,
            String endDate,
            String category,
            Double minAmount,
            Double maxAmount,
            String keyword,
            Integer page,
            Integer size
    ) {
        String u = username == null ? "" : username.trim();
        if (u.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        if (p < 0) p = 0;
        if (s < 1) s = 20;
        if (s > 200) s = 200;

        String prov = provider == null ? "ALL" : provider.trim().toUpperCase();
        boolean isAllProvider = prov.isBlank() || "ALL".equalsIgnoreCase(prov);

        LocalDate sd = parseIsoDateOrNull(startDate);
        LocalDate ed = parseIsoDateOrNull(endDate);
        if (sd != null && ed != null && sd.isAfter(ed)) {
            throw new IllegalArgumentException("기간이 올바르지 않습니다. (시작일이 종료일보다 늦습니다.)");
        }

        String cat = category == null ? "" : category.trim();
        String kw = keyword == null ? "" : keyword.trim();

        Double min = minAmount;
        Double max = maxAmount;
        if (min != null && min < 0) min = 0.0;
        if (max != null && max < 0) max = 0.0;
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException("금액 범위가 올바르지 않습니다. (최소 금액이 최대 금액보다 큽니다.)");
        }

        final Double finalMin = min;
        final Double finalMax = max;

        Specification<Transaction> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("createdBy"), u));
            predicates.add(cb.equal(root.get("confirmed"), "Y"));

            if (!isAllProvider) {
                predicates.add(cb.equal(cb.upper(root.get("provider")), prov));
            }

            if (sd != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("txDate"), sd));
            }
            if (ed != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("txDate"), ed));
            }

            if (!cat.isBlank()) {
                predicates.add(cb.equal(root.get("category"), cat));
            }

            if (finalMin != null || finalMax != null) {
                jakarta.persistence.criteria.Expression<Double> absAmt = cb.abs(root.get("amount")).as(Double.class);
                if (finalMin != null) {
                    predicates.add(cb.greaterThanOrEqualTo(absAmt, finalMin));
                }
                if (finalMax != null) {
                    predicates.add(cb.lessThanOrEqualTo(absAmt, finalMax));
                }
            }

            if (!kw.isBlank()) {
                String like = "%" + kw.toLowerCase() + "%";
                jakarta.persistence.criteria.Expression<String> desc = cb.lower(root.get("description"));
                jakarta.persistence.criteria.Expression<String> detail = cb.lower(root.get("txDetail"));
                predicates.add(cb.or(
                        cb.like(desc, like),
                        cb.like(cb.coalesce(detail, ""), like)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(p, s, Sort.by(Sort.Order.desc("txDate"), Sort.Order.desc("id")));
        Page<Transaction> result = transactionRepository.findAll(spec, pageable);

        List<TransactionSearchResponse.TransactionItem> items = new ArrayList<>();
        if (result.getContent() != null) {
            for (Transaction t : result.getContent()) {
                if (t == null) continue;
                items.add(new TransactionSearchResponse.TransactionItem(
                        t.getId(),
                        t.getProvider(),
                        t.getTxDate() == null ? null : t.getTxDate().toString(),
                        t.getDescription(),
                        t.getTxType(),
                        t.getTxDetail(),
                        t.getCategory(),
                        t.getAmount() == null ? null : t.getAmount().doubleValue(),
                        t.getPostBalance() == null ? null : t.getPostBalance().doubleValue()
                ));
            }
        }

        return TransactionSearchResponse.ok(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                items
        );
    }

    private LocalDate parseIsoDateOrNull(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isBlank()) return null;
        try {
            return LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignore) {
            throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다. (YYYY-MM-DD)");
        }
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
            String txType = r.getTxType() == null ? "" : r.getTxType().trim();
            String txDetail = r.getTxDetail() == null ? "" : r.getTxDetail().trim();
            Double amountDouble = r.getAmount();
            Double postBalanceDouble = r.getPostBalance();

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
            t.setTxType(txType.isBlank() ? null : txType);
            t.setTxDetail(txDetail.isBlank() ? null : txDetail);
            t.setAmount(BigDecimal.valueOf(amountDouble));
            if (postBalanceDouble != null) {
                t.setPostBalance(BigDecimal.valueOf(postBalanceDouble));
            }
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
            r.setTxType(t.getTxType());
            r.setTxDetail(t.getTxDetail());
            r.setPostBalance(t.getPostBalance() == null ? null : t.getPostBalance().doubleValue());
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

    @Transactional
    public long unconfirmMonth(String provider, Integer year, Integer month, String username) {
        String p = provider == null ? "" : provider.trim().toUpperCase();
        if (p.isBlank()) {
            throw new IllegalArgumentException("은행/카드사를 선택해주세요.");
        }
        if (year == null || month == null) {
            throw new IllegalArgumentException("취소할 년/월이 올바르지 않습니다.");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("취소할 년/월이 올바르지 않습니다.");
        }
        String u = username == null ? "" : username.trim();
        if (u.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        return transactionRepository.deleteByProviderAndTxYearAndTxMonthAndConfirmedAndCreatedBy(
                p,
                year,
                month,
                "Y",
                u
        );
    }
}
