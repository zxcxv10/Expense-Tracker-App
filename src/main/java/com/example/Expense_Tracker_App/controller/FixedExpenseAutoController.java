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

import com.example.Expense_Tracker_App.dto.FixedExpenseAutoConfirmResponse;
import com.example.Expense_Tracker_App.dto.FixedExpenseAutoGenerateResponse;
import com.example.Expense_Tracker_App.dto.FixedExpenseAutoGenerateResult;
import com.example.Expense_Tracker_App.dto.FixedExpenseAutoMonthTransactionsResponse;
import com.example.Expense_Tracker_App.dto.FixedExpenseAutoSettingResponse;
import com.example.Expense_Tracker_App.dto.FixedExpenseAutoSettingUpdateRequest;
import com.example.Expense_Tracker_App.dto.FixedExpenseAutoMonthTxItem;
import com.example.Expense_Tracker_App.dto.FixedExpenseAutoUnconfirmResponse;
import com.example.Expense_Tracker_App.entity.FixedExpenseAutoSetting;
import com.example.Expense_Tracker_App.service.FixedExpenseAutoService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/fixed-expenses/auto")
public class FixedExpenseAutoController {

    private final FixedExpenseAutoService autoService;

    public FixedExpenseAutoController(FixedExpenseAutoService autoService) {
        this.autoService = autoService;
    }

    @PostMapping(value = "/unconfirm", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedExpenseAutoUnconfirmResponse> unconfirm(
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
            return ResponseEntity.ok(FixedExpenseAutoUnconfirmResponse.ok(y, m, unconfirmed, msg));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedExpenseAutoUnconfirmResponse.fail(e.getMessage()));
        }
    }

    @GetMapping(value = "/setting", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedExpenseAutoSettingResponse> getSetting(HttpSession session) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            FixedExpenseAutoSetting s = autoService.getSetting(username);
            return ResponseEntity.ok(FixedExpenseAutoSettingResponse.ok(
                    s.getEnabled(),
                    s.getLastRunAt() == null ? null : s.getLastRunAt().toString(),
                    s.getLastRunMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedExpenseAutoSettingResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(value = "/setting", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedExpenseAutoSettingResponse> updateSetting(
            @RequestBody FixedExpenseAutoSettingUpdateRequest req,
            HttpSession session
    ) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            FixedExpenseAutoSetting s = autoService.updateEnabled(username, req == null ? null : req.getEnabled());
            return ResponseEntity.ok(FixedExpenseAutoSettingResponse.ok(
                    s.getEnabled(),
                    s.getLastRunAt() == null ? null : s.getLastRunAt().toString(),
                    s.getLastRunMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedExpenseAutoSettingResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedExpenseAutoGenerateResponse> generate(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            HttpSession session
    ) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");

            LocalDate now = LocalDate.now();
            int y = year == null ? now.getYear() : year;
            int m = month == null ? now.getMonthValue() : month;

            FixedExpenseAutoGenerateResult result = autoService.generateForUser(username, y, m, true);
            return ResponseEntity.ok(FixedExpenseAutoGenerateResponse.ok(y, m, result.getCreated(), result.getSkipped(), result.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedExpenseAutoGenerateResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(value = "/confirm", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedExpenseAutoConfirmResponse> confirm(
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
            return ResponseEntity.ok(FixedExpenseAutoConfirmResponse.ok(y, m, confirmed, msg));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedExpenseAutoConfirmResponse.fail(e.getMessage()));
        }
    }

    @GetMapping(value = "/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedExpenseAutoMonthTransactionsResponse> transactions(
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
            for (FixedExpenseAutoMonthTxItem it : items) {
                if (it != null && "Y".equalsIgnoreCase(String.valueOf(it.getConfirmed()))) confirmed++;
                else unconfirmed++;
            }
            return ResponseEntity.ok(FixedExpenseAutoMonthTransactionsResponse.ok(y, m, items, unconfirmed, confirmed));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedExpenseAutoMonthTransactionsResponse.fail(e.getMessage()));
        }
    }
}
