package com.example.Expense_Tracker_App.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Expense_Tracker_App.dto.LoanPaymentResponse;
import com.example.Expense_Tracker_App.service.LoanPaymentHistoryService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/loan-payments")
public class LoanPaymentHistoryController {

    private final LoanPaymentHistoryService loanPaymentHistoryService;

    public LoanPaymentHistoryController(LoanPaymentHistoryService loanPaymentHistoryService) {
        this.loanPaymentHistoryService = loanPaymentHistoryService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoanPaymentResponse> list(
            @RequestParam("ym") String ym,
            HttpSession session
    ) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(LoanPaymentResponse.okList(loanPaymentHistoryService.getList(username, ym)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(LoanPaymentResponse.fail(e.getMessage()));
        }
    }
}
