package com.example.Expense_Tracker_App.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Expense_Tracker_App.dto.TransactionSearchResponse;
import com.example.Expense_Tracker_App.service.TransactionService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/transactions")
public class TransactionSearchController {

    private final TransactionService transactionService;

    public TransactionSearchController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransactionSearchResponse> search(
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "minAmount", required = false) Double minAmount,
            @RequestParam(value = "maxAmount", required = false) Double maxAmount,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            HttpSession session
    ) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(transactionService.searchConfirmedTransactions(
                    username,
                    provider,
                    startDate,
                    endDate,
                    category,
                    minAmount,
                    maxAmount,
                    keyword,
                    page,
                    size
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(TransactionSearchResponse.fail(e.getMessage()));
        }
    }
}
