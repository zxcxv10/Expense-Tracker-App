package com.example.Expense_Tracker_App.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.Expense_Tracker_App.dto.ConfirmedTransactionsResponse;
import com.example.Expense_Tracker_App.dto.ImportConfirmRequest;
import com.example.Expense_Tracker_App.dto.ImportConfirmResponse;
import com.example.Expense_Tracker_App.dto.ImportPreviewResponse;
import com.example.Expense_Tracker_App.dto.ImportPreviewRow;
import com.example.Expense_Tracker_App.service.ImportPreviewService;
import com.example.Expense_Tracker_App.service.TransactionService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ImportPreviewService importPreviewService;
    private final TransactionService transactionService;

    public ImportController(ImportPreviewService importPreviewService, TransactionService transactionService) {
        this.importPreviewService = importPreviewService;
        this.transactionService = transactionService;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportPreviewResponse> preview(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam(value = "pdfPassword", required = false) String pdfPassword
    ) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(ImportPreviewResponse.fail("파일이 비어있습니다."));
            }

            List<ImportPreviewRow> rows = importPreviewService.parsePreview(file, provider, pdfPassword);
            return ResponseEntity.ok(ImportPreviewResponse.ok(rows));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ImportPreviewResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(value = "/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImportConfirmResponse> confirm(@RequestBody ImportConfirmRequest request, HttpSession session) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body(ImportConfirmResponse.fail("요청 데이터가 비어있습니다."));
            }

            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            int inserted = transactionService.confirmImport(
                    request.getProvider(),
                    username,
                    request.getRows()
            );
            return ResponseEntity.ok(ImportConfirmResponse.ok(inserted));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ImportConfirmResponse.fail(e.getMessage()));
        }
    }

    @GetMapping(value = "/confirmed", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConfirmedTransactionsResponse> confirmed(
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month
            , HttpSession session
    ) {
        try {
            String username = session == null ? null : (String) session.getAttribute("USERNAME");
            boolean confirmed = transactionService.isConfirmed(provider, year, month, username);
            List<ImportPreviewRow> rows = confirmed
                    ? transactionService.getConfirmedRows(provider, year, month, username)
                    : List.of();
            return ResponseEntity.ok(ConfirmedTransactionsResponse.ok(confirmed, rows));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ConfirmedTransactionsResponse.fail(e.getMessage()));
        }
    }
}
