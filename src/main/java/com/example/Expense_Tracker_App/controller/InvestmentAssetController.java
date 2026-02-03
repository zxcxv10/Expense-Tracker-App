package com.example.Expense_Tracker_App.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Expense_Tracker_App.dto.InvestmentAssetRequest;
import com.example.Expense_Tracker_App.dto.InvestmentAssetResponse;
import com.example.Expense_Tracker_App.service.InvestmentAssetService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/investment-assets")
public class InvestmentAssetController {

    private final InvestmentAssetService investmentAssetService;

    public InvestmentAssetController(InvestmentAssetService investmentAssetService) {
        this.investmentAssetService = investmentAssetService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InvestmentAssetResponse> list(HttpSession session) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(InvestmentAssetResponse.okList(investmentAssetService.getList(username)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(InvestmentAssetResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InvestmentAssetResponse> create(@RequestBody InvestmentAssetRequest request, HttpSession session) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(InvestmentAssetResponse.okItem(investmentAssetService.create(username, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(InvestmentAssetResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InvestmentAssetResponse> update(@RequestBody InvestmentAssetRequest request, HttpSession session) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(InvestmentAssetResponse.okItem(investmentAssetService.update(username, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(InvestmentAssetResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InvestmentAssetResponse> delete(@PathVariable("id") Long id, HttpSession session) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(InvestmentAssetResponse.okDeleted(investmentAssetService.delete(username, id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(InvestmentAssetResponse.fail(e.getMessage()));
        }
    }
}
