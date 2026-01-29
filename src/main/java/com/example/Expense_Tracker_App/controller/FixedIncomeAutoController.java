package com.example.Expense_Tracker_App.controller;

import java.time.LocalDate;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Expense_Tracker_App.dto.FixedIncomeAutoConfirmResponse;
import com.example.Expense_Tracker_App.dto.FixedIncomeAutoGenerateResponse;
import com.example.Expense_Tracker_App.dto.FixedIncomeAutoGenerateResult;
import com.example.Expense_Tracker_App.dto.FixedIncomeAutoMonthTransactionsResponse;
import com.example.Expense_Tracker_App.dto.FixedIncomeAutoMonthTxItem;
import com.example.Expense_Tracker_App.dto.FixedIncomeAutoSettingResponse;
import com.example.Expense_Tracker_App.dto.FixedIncomeAutoSettingUpdateRequest;
import com.example.Expense_Tracker_App.dto.FixedIncomeAutoUnconfirmResponse;
import com.example.Expense_Tracker_App.entity.FixedIncomeAutoSetting;
import com.example.Expense_Tracker_App.service.FixedIncomeAutoService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/fixed-incomes/auto")
public class FixedIncomeAutoController {

    private final FixedIncomeAutoService autoService;

    public FixedIncomeAutoController(FixedIncomeAutoService autoService) {
        this.autoService = autoService;
    }

    @PostMapping(value = "/unconfirm", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedIncomeAutoUnconfirmResponse> unconfirm(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            HttpSession session
    ) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");

            LocalDate now = LocalDate.now();
            int y = year == null ? now.getYear() : year;
            int m = month == null ? now.getMonthValue() : month;

            int unconfirmed = autoService.unconfirmForUser(username, y, m);
            String msg = unconfirmed == 0 ? "취소할 내역이 없습니다." : String.format("%d건 확정 취소", unconfirmed);
            return ResponseEntity.ok(FixedIncomeAutoUnconfirmResponse.ok(y, m, unconfirmed, msg));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedIncomeAutoUnconfirmResponse.fail(e.getMessage()));
        }
    }

    @GetMapping(value = "/setting", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedIncomeAutoSettingResponse> getSetting(HttpSession session) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            FixedIncomeAutoSetting s = autoService.getSetting(username);
            return ResponseEntity.ok(FixedIncomeAutoSettingResponse.ok(
                    s.getEnabled(),
                    s.getLastRunAt() == null ? null : s.getLastRunAt().toString(),
                    s.getLastRunMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedIncomeAutoSettingResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(value = "/setting", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedIncomeAutoSettingResponse> updateSetting(
            @RequestBody FixedIncomeAutoSettingUpdateRequest req,
            HttpSession session
    ) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            FixedIncomeAutoSetting s = autoService.updateEnabled(username, req == null ? null : req.getEnabled());
            return ResponseEntity.ok(FixedIncomeAutoSettingResponse.ok(
                    s.getEnabled(),
                    s.getLastRunAt() == null ? null : s.getLastRunAt().toString(),
                    s.getLastRunMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedIncomeAutoSettingResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedIncomeAutoGenerateResponse> generate(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            HttpSession session
    ) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");

            LocalDate now = LocalDate.now();
            int y = year == null ? now.getYear() : year;
            int m = month == null ? now.getMonthValue() : month;

            FixedIncomeAutoGenerateResult result = autoService.generateForUser(username, y, m, true);
            return ResponseEntity.ok(FixedIncomeAutoGenerateResponse.ok(y, m, result.getCreated(), result.getSkipped(), result.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedIncomeAutoGenerateResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(value = "/confirm", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedIncomeAutoConfirmResponse> confirm(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            HttpSession session
    ) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");

            LocalDate now = LocalDate.now();
            int y = year == null ? now.getYear() : year;
            int m = month == null ? now.getMonthValue() : month;

            int confirmed = autoService.confirmForUser(username, y, m);
            String msg = confirmed == 0 ? "확정할 내역이 없습니다." : String.format("%d건 확정 완료", confirmed);
            return ResponseEntity.ok(FixedIncomeAutoConfirmResponse.ok(y, m, confirmed, msg));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedIncomeAutoConfirmResponse.fail(e.getMessage()));
        }
    }

    @GetMapping(value = "/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedIncomeAutoMonthTransactionsResponse> transactions(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            HttpSession session
    ) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");

            LocalDate now = LocalDate.now();
            int y = year == null ? now.getYear() : year;
            int m = month == null ? now.getMonthValue() : month;

            var items = autoService.listFixedTransactionsForMonth(username, y, m);
            int unconfirmed = 0;
            int confirmed = 0;
            for (FixedIncomeAutoMonthTxItem it : items) {
                if (it != null && "Y".equalsIgnoreCase(String.valueOf(it.getConfirmed()))) confirmed++;
                else unconfirmed++;
            }
            return ResponseEntity.ok(FixedIncomeAutoMonthTransactionsResponse.ok(y, m, items, unconfirmed, confirmed));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedIncomeAutoMonthTransactionsResponse.fail(e.getMessage()));
        }
    }
}
