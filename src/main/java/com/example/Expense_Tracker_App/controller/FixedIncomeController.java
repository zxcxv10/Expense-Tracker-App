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

import com.example.Expense_Tracker_App.dto.FixedIncomeRequest;
import com.example.Expense_Tracker_App.dto.FixedIncomeResponse;
import com.example.Expense_Tracker_App.service.FixedIncomeService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/fixed-incomes")
public class FixedIncomeController {

    private final FixedIncomeService fixedIncomeService;

    public FixedIncomeController(FixedIncomeService fixedIncomeService) {
        this.fixedIncomeService = fixedIncomeService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedIncomeResponse> list(
            @RequestParam(value = "status", required = false) String status,
            HttpSession session
    ) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(FixedIncomeResponse.okList(fixedIncomeService.getList(username, status)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedIncomeResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedIncomeResponse> create(@RequestBody FixedIncomeRequest request, HttpSession session) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(FixedIncomeResponse.okItem(fixedIncomeService.create(username, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedIncomeResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedIncomeResponse> update(@RequestBody FixedIncomeRequest request, HttpSession session) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(FixedIncomeResponse.okItem(fixedIncomeService.update(username, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedIncomeResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FixedIncomeResponse> delete(@PathVariable("id") Long id, HttpSession session) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            return ResponseEntity.ok(FixedIncomeResponse.okDeleted(fixedIncomeService.delete(username, id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(FixedIncomeResponse.fail(e.getMessage()));
        }
    }
}
