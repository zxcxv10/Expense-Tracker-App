package com.example.Expense_Tracker_App.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Expense_Tracker_App.dto.LoanRequest;
import com.example.Expense_Tracker_App.dto.LoanResponse;
import com.example.Expense_Tracker_App.service.LoanService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoanResponse> list(HttpSession session) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(LoanResponse.okList(loanService.getList(username)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(LoanResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoanResponse> create(@RequestBody LoanRequest request, HttpSession session) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(LoanResponse.okItem(loanService.create(username, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(LoanResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoanResponse> update(@RequestBody LoanRequest request, HttpSession session) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(LoanResponse.okItem(loanService.update(username, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(LoanResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoanResponse> delete(@PathVariable("id") Long id, HttpSession session) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(LoanResponse.okDeleted(loanService.delete(username, id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(LoanResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(value = "/{id}/pay-monthly", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoanResponse> payMonthly(
            @PathVariable("id") Long id,
            @RequestParam(value = "ym", required = false) String ym,
            HttpSession session
    ) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(LoanResponse.okItem(loanService.payMonthly(username, id, ym)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(LoanResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(value = "/dedupe", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoanResponse> dedupe(HttpSession session) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(LoanResponse.okDeleted(loanService.dedupe(username)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(LoanResponse.fail(e.getMessage()));
        }
    }
}
