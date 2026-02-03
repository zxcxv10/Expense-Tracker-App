package com.example.Expense_Tracker_App.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Expense_Tracker_App.dto.InvestmentAssetRequest;
import com.example.Expense_Tracker_App.dto.InvestmentAssetResponse.InvestmentAssetItem;
import com.example.Expense_Tracker_App.entity.InvestmentAsset;
import com.example.Expense_Tracker_App.repository.InvestmentAssetRepository;

@Service
public class InvestmentAssetService {

    private final InvestmentAssetRepository investmentAssetRepository;

    public InvestmentAssetService(InvestmentAssetRepository investmentAssetRepository) {
        this.investmentAssetRepository = investmentAssetRepository;
    }

    @Transactional(readOnly = true)
    public List<InvestmentAssetItem> getList(String username) {
        String u = requireUser(username);
        List<InvestmentAsset> list = investmentAssetRepository.findByUsernameOrderByAccountNameAscAssetTypeAscAssetNameAscIdDesc(u);
        List<InvestmentAssetItem> items = new ArrayList<>();
        for (InvestmentAsset e : list) {
            items.add(toItem(e));
        }
        return items;
    }

    @Transactional
    public InvestmentAssetItem create(String username, InvestmentAssetRequest request) {
        String u = requireUser(username);
        if (request == null) {
            throw new IllegalArgumentException("요청 데이터가 비어있습니다.");
        }

        InvestmentAsset entity = new InvestmentAsset();
        applyRequest(entity, request, u, true);
        InvestmentAsset saved = investmentAssetRepository.save(entity);
        return toItem(saved);
    }

    @Transactional
    public InvestmentAssetItem update(String username, InvestmentAssetRequest request) {
        String u = requireUser(username);
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("수정할 항목을 선택해주세요.");
        }

        InvestmentAsset entity = investmentAssetRepository.findByIdAndUsername(request.getId(), u)
                .orElseThrow(() -> new IllegalArgumentException("해당 항목을 찾을 수 없습니다."));
        applyRequest(entity, request, u, false);
        InvestmentAsset saved = investmentAssetRepository.save(entity);
        return toItem(saved);
    }

    @Transactional
    public long delete(String username, Long id) {
        String u = requireUser(username);
        if (id == null) {
            throw new IllegalArgumentException("삭제할 항목을 선택해주세요.");
        }
        return investmentAssetRepository.deleteByIdAndUsername(id, u);
    }

    private void applyRequest(InvestmentAsset entity, InvestmentAssetRequest request, String username, boolean isCreate) {
        String accountName = safeTrim(request.getAccountName());
        String assetType = safeTrim(request.getAssetType());
        if (assetType.isBlank()) {
            throw new IllegalArgumentException("분류(assetType)를 입력해주세요.");
        }
        String assetName = safeTrim(request.getAssetName());
        if (assetName.isBlank()) {
            throw new IllegalArgumentException("자산명(assetName)을 입력해주세요.");
        }

        Double quantity = request.getQuantity();
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("수량(quantity)은 0 이상이어야 합니다.");
        }

        Double avgBuyPrice = request.getAvgBuyPrice();
        if (avgBuyPrice != null && avgBuyPrice < 0) {
            throw new IllegalArgumentException("평단가(avgBuyPrice)는 0 이상이어야 합니다.");
        }

        Double evaluatedAmount = request.getEvaluatedAmount();
        if (evaluatedAmount == null) {
            throw new IllegalArgumentException("현재 평가금액을 입력해주세요.");
        }
        if (evaluatedAmount < 0) {
            throw new IllegalArgumentException("현재 평가금액은 0 이상이어야 합니다.");
        }

        Double costAmount = request.getCostAmount();
        if (costAmount != null && costAmount < 0) {
            throw new IllegalArgumentException("매입금액은 0 이상이어야 합니다.");
        }

        BigDecimal costAmountValue = costAmount == null ? null : BigDecimal.valueOf(costAmount);
        if (costAmountValue == null && quantity != null && avgBuyPrice != null) {
            costAmountValue = BigDecimal.valueOf(quantity).multiply(BigDecimal.valueOf(avgBuyPrice));
        }

        entity.setUsername(username);
        entity.setAccountName(accountName.isBlank() ? null : accountName);
        entity.setAssetType(assetType);
        entity.setAssetName(assetName);
        entity.setQuantity(quantity == null ? null : BigDecimal.valueOf(quantity));
        entity.setAvgBuyPrice(avgBuyPrice == null ? null : BigDecimal.valueOf(avgBuyPrice));
        entity.setEvaluatedAmount(BigDecimal.valueOf(evaluatedAmount));
        entity.setCostAmount(costAmountValue);
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

    private InvestmentAssetItem toItem(InvestmentAsset e) {
        InvestmentAssetItem item = new InvestmentAssetItem();
        item.setId(e.getId());
        item.setAccountName(e.getAccountName());
        item.setAssetType(e.getAssetType());
        item.setAssetName(e.getAssetName());
        item.setQuantity(e.getQuantity() == null ? null : e.getQuantity().doubleValue());
        item.setAvgBuyPrice(e.getAvgBuyPrice() == null ? null : e.getAvgBuyPrice().doubleValue());
        item.setEvaluatedAmount(e.getEvaluatedAmount() == null ? null : e.getEvaluatedAmount().doubleValue());
        item.setCostAmount(e.getCostAmount() == null ? null : e.getCostAmount().doubleValue());
        item.setMemo(e.getMemo());
        item.setCreatedAt(e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
        item.setUpdatedAt(e.getUpdatedAt() == null ? null : e.getUpdatedAt().toString());
        return item;
    }
}
