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

import com.example.Expense_Tracker_App.dto.FixedExpenseRequest;
import com.example.Expense_Tracker_App.dto.FixedExpenseResponse;
import com.example.Expense_Tracker_App.service.FixedExpenseService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/fixed-expenses")
public class FixedExpenseController {

    private final FixedExpenseService fixedExpenseService;

    public FixedExpenseController(FixedExpenseService fixedExpenseService) {
        this.fixedExpenseService = fixedExpenseService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedExpenseResponse> list(
            @RequestParam(value = "status", required = false) String status,
            HttpSession session
    ) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(FixedExpenseResponse.okList(fixedExpenseService.getList(username, status)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedExpenseResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedExpenseResponse> create(@RequestBody FixedExpenseRequest request, HttpSession session) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(FixedExpenseResponse.okItem(fixedExpenseService.create(username, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedExpenseResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedExpenseResponse> update(@RequestBody FixedExpenseRequest request, HttpSession session) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(FixedExpenseResponse.okItem(fixedExpenseService.update(username, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedExpenseResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedExpenseResponse> delete(@PathVariable("id") Long id, HttpSession session) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(FixedExpenseResponse.okDeleted(fixedExpenseService.delete(username, id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedExpenseResponse.fail(e.getMessage()));
        }
    }
}
