package com.example.Expense_Tracker_App.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Expense_Tracker_App.dto.DashboardCategoryResponse;
import com.example.Expense_Tracker_App.dto.DashboardMonthlyResponse;
import com.example.Expense_Tracker_App.service.DashboardService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping(value = "/monthly", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DashboardMonthlyResponse> monthly(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "provider", required = false) String provider,
            HttpSession session
    ) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(dashboardService.getMonthly(year, provider, username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(DashboardMonthlyResponse.fail(e.getMessage()));
        }
    }

    @GetMapping(value = "/categories", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DashboardCategoryResponse> categories(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "provider", required = false) String provider,
            HttpSession session
    ) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            Map<String, Object> data = dashboardService.getCategories(year, month, provider, username);
            @SuppressWarnings("unchecked")
            Map<String, Double> income = (Map<String, Double>) data.get("incomeByCategory");
            @SuppressWarnings("unchecked")
            Map<String, Double> expense = (Map<String, Double>) data.get("expenseByCategory");
            String p = (String) data.get("provider");
            return ResponseEntity.ok(DashboardCategoryResponse.ok(year, month, p, income, expense));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(DashboardCategoryResponse.fail(e.getMessage()));
        }
    }
}
