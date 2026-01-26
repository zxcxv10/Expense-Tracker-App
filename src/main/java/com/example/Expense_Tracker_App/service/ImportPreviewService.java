package com.example.Expense_Tracker_App.service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.Expense_Tracker_App.dto.ImportPreviewRow;

@Service
public class ImportPreviewService {

    private static final Logger log = LoggerFactory.getLogger(ImportPreviewService.class);

    private static final Pattern TX_LINE = Pattern.compile(
            "^(\\d{4}[./-]\\d{2}[./-]\\d{2})(?:\\s+\\d{2}:\\d{2}:\\d{2})?\\s+(.+)$");
    private static final Pattern KB_TX_DT = Pattern.compile(
            "^(\\d{4})\\.(\\d{2})\\.(\\d{2})\\s+(\\d{2}):(\\d{2}):(\\d{2})\\s+(.+)$");
    private static final Pattern AMOUNT_TOKEN = Pattern.compile("^(?:0|[+-]?\\d{1,3}(?:,\\d{3})*)$");
    private static final Pattern URL_TOKEN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);

    private static final Pattern HYUNDAI_MERCHANT_AMOUNT = Pattern.compile(
            "^(.+?)\\s+([0-9]{1,3}(?:,[0-9]{3})*)원$");
    private static final Pattern HYUNDAI_DATE_LINE = Pattern.compile(
            "^(\\d{2})\\.\\s*(\\d{2})\\.\\s*(\\d{2})\\b.*$");

    private static final Pattern NH_DATE_TOKEN = Pattern.compile("^\\d{4}[./-]\\d{2}[./-]\\d{2}$");
    private static final Pattern NH_TIME_TOKEN = Pattern.compile("^\\d{2}:\\d{2}:\\d{2}$");
    private static final Pattern NH_AMOUNT_TOKEN = Pattern.compile("^(?:0|\\d{1,3}(?:,\\d{3})*)원$");

    private static class NhTxTemp {
        private final LocalDateTime dt;
        private final String dateIso;
        private final Double withdraw;
        private final Double deposit;
        private final Double balance;
        private final String description;

        private NhTxTemp(LocalDateTime dt, String dateIso, Double withdraw, Double deposit, Double balance, String description) {
            this.dt = dt;
            this.dateIso = dateIso;
            this.withdraw = withdraw;
            this.deposit = deposit;
            this.balance = balance;
            this.description = description;
        }
    }

    private static class PdfTxBlock {
        private final String dateIso;
        private final String block;

        private PdfTxBlock(String dateIso, String block) {
            this.dateIso = dateIso;
            this.block = block;
        }
    }

    public List<ImportPreviewRow> parsePreview(MultipartFile file, String provider, String pdfPassword) throws Exception {
        String filename = file.getOriginalFilename();
        String ext = getExtension(filename);

        log.info("[import-preview] start filename={} ext={} provider={} pdfPasswordProvided={}",
                filename,
                ext,
                provider,
                pdfPassword != null && !pdfPassword.isBlank());

        if (ext.equals(".pdf")) {
            List<ImportPreviewRow> rows = parsePdf(file, provider, pdfPassword);
            logPreview("pdf", rows);
            return ensureNotEmpty(rows, "PDF 파싱 결과가 없습니다.");
        }

        
        throw new IllegalArgumentException("지원하지 않는 파일 형식입니다: " + ext);
    }

    private List<PdfTxBlock> groupKbTxBlocks(String text) {
        List<PdfTxBlock> blocks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return blocks;
        }

        String[] lines = text.split("\\R");
        String currentDateIso = null;
        StringBuilder currentBlock = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank()) {
                continue;
            }

            Matcher m = KB_TX_DT.matcher(line);
            if (m.matches()) {
                if (currentDateIso != null) {
                    blocks.add(new PdfTxBlock(currentDateIso, currentBlock.toString()));
                }

                String dateIso = m.group(1) + "-" + m.group(2) + "-" + m.group(3);
                currentDateIso = dateIso;
                currentBlock.setLength(0);
                currentBlock.append(m.group(7));
            } else {
                if (currentDateIso != null) {
                    currentBlock.append(' ').append(line);
                }
            }
        }

        if (currentDateIso != null) {
            blocks.add(new PdfTxBlock(currentDateIso, currentBlock.toString()));
        }

        return blocks;
    }

    private List<ImportPreviewRow> parseTossPdf(String text) {
        List<ImportPreviewRow> rows = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return rows;
        }

        List<PdfTxBlock> blocks = groupPdfTxBlocks(text);
        int matched = 0;
        for (PdfTxBlock b : blocks) {
            ImportPreviewRow row = buildRowFromTossBlock(b.dateIso, b.block);
            if (row != null) {
                rows.add(row);
                matched++;
            }
        }

        log.info("[import-preview][pdf] toss-parser groupedTx={} matchedRows={}", blocks.size(), matched);
        return rows;
    }

    private List<ImportPreviewRow> parseKbPdf(String text) {
        List<ImportPreviewRow> rows = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return rows;
        }

        List<PdfTxBlock> blocks = groupKbTxBlocks(text);
        int matched = 0;
        for (PdfTxBlock b : blocks) {
            ImportPreviewRow row = buildRowFromKbBlock(b.dateIso, b.block);
            if (row != null) {
                rows.add(row);
                matched++;
            }
        }

        log.info("[import-preview][pdf] kb-parser groupedTx={} matchedRows={}", blocks.size(), matched);
        return rows;
    }

    private List<ImportPreviewRow> parseHyundaiPdf(String text) {
        List<ImportPreviewRow> rows = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return rows;
        }

        String[] lines = text.split("\\R");
        String lastMerchant = null;
        Double lastAmount = null;
        int matched = 0;

        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank()) {
                continue;
            }

            // 현대 PDF에 포함될 수 있는 불필요한 노이즈 라인은 스킵
            if (URL_TOKEN.matcher(line).find()) {
                continue;
            }

            Matcher ma = HYUNDAI_MERCHANT_AMOUNT.matcher(line);
            if (ma.matches()) {
                String merchant = ma.group(1) == null ? "" : ma.group(1).trim();
                Double amount = parseAmountToken(ma.group(2));
                if (!merchant.isBlank() && amount != null) {
                    lastMerchant = merchant;
                    lastAmount = amount;
                }
                continue;
            }

            Matcher dm = HYUNDAI_DATE_LINE.matcher(line);
            if (dm.matches() && lastMerchant != null && lastAmount != null) {
                int yy = Integer.parseInt(dm.group(1));
                int mm = Integer.parseInt(dm.group(2));
                int dd = Integer.parseInt(dm.group(3));
                int yyyy = (yy <= 69) ? 2000 + yy : 1900 + yy;
                String dateIso = String.format("%04d-%02d-%02d", yyyy, mm, dd);

                if (isValidIsoDate(dateIso)) {
                    double amount = (lastAmount == 0.0) ? 0.0 : -Math.abs(lastAmount);
                    rows.add(new ImportPreviewRow(dateIso, lastMerchant, amount, "카드결제"));
                    matched++;
                }

                lastMerchant = null;
                lastAmount = null;
            }
        }

        if (matched == 0) {
            StringBuilder sb = new StringBuilder();
            int appended = 0;
            for (String rawLine : lines) {
                String line = rawLine == null ? "" : rawLine.trim();
                if (line.isBlank()) {
                    continue;
                }
                sb.append(line);
                sb.append(" | ");
                appended++;
                if (appended >= 30) {
                    break;
                }
            }
            log.info("[import-preview][pdf] hyundai-parser no-match sampleLines={}", sb.toString());
        }

        log.info("[import-preview][pdf] hyundai-parser matchedRows={}", matched);
        return rows;
    }

    private List<ImportPreviewRow> parseNhPdf(String text) {
        List<ImportPreviewRow> rows = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return rows;
        }

        // PDFBox로 텍스트를 뽑으면 표가 줄바꿈/공백으로 찢어지는 케이스가 많아서,
        // 농협은 "YYYY/MM/DD" + "HH:MM:SS" 토큰을 기준으로 거래 블록을 잡는다.
        String[] lines = text.split("\\R");
        List<String> tokens = new ArrayList<>();
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank()) continue;
            String[] parts = line.split("\\s+");
            for (String part : parts) {
                if (part == null) continue;
                String t = part.trim();
                if (t.isBlank()) continue;
                tokens.add(t);
            }
        }

        List<NhTxTemp> temp = new ArrayList<>();
        int matched = 0;
        for (int i = 0; i + 1 < tokens.size(); i++) {
            String dTok = tokens.get(i);
            String tTok = tokens.get(i + 1);
            if (!NH_DATE_TOKEN.matcher(dTok).matches()) continue;
            if (!NH_TIME_TOKEN.matcher(tTok).matches()) continue;

            String dateIso = normalizeDate(dTok);
            if (!isValidIsoDate(dateIso)) continue;

            LocalDateTime dt;
            try {
                String normalizedDateTime = (dTok.replace('.', '/').replace('-', '/')) + " " + tTok;
                dt = LocalDateTime.parse(normalizedDateTime, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
            } catch (Exception e) {
                continue;
            }

            int segStart = i + 2;
            int segEnd = tokens.size();
            for (int j = segStart; j + 1 < tokens.size(); j++) {
                if (NH_DATE_TOKEN.matcher(tokens.get(j)).matches() && NH_TIME_TOKEN.matcher(tokens.get(j + 1)).matches()) {
                    segEnd = j;
                    break;
                }
            }

            // amount tokens in NH statement always include trailing "원".
            // We treat the LAST amount token in the segment as the balance, and compute tx amount later by balance deltas.
            List<Integer> amtIdx = new ArrayList<>();
            for (int j = segStart; j < segEnd; j++) {
                if (isNhAmountToken(tokens.get(j))) {
                    amtIdx.add(j);
                }
            }
            if (amtIdx.isEmpty()) {
                continue;
            }

            int balanceIdx = amtIdx.get(amtIdx.size() - 1);
            Double balance = parseNhAmountToken(tokens.get(balanceIdx));
            if (balance == null) {
                continue;
            }

            Double withdraw = null;
            Double deposit = null;
            if (amtIdx.size() >= 3) {
                // best effort: if we have 3+ amounts, assume first two are withdraw/deposit columns
                withdraw = parseNhAmountToken(tokens.get(amtIdx.get(0)));
                deposit = parseNhAmountToken(tokens.get(amtIdx.get(1)));
            } else if (amtIdx.size() == 2) {
                // 출금/입금 중 한쪽이 비어있는 케이스(금액 토큰 2개):
                // [입/출금] + [잔액] 형태로 남는 경우가 많아, 첫 번째 값을 입금으로 보정(마지막 행 fallback용)
                deposit = parseNhAmountToken(tokens.get(amtIdx.get(0)));
            }

            String description = "";
            int descStart = balanceIdx + 1;
            if (descStart < segEnd) {
                description = extractNhDescription(tokens, descStart, segEnd);
            }
            if (description.isBlank()) {
                description = "거래내역";
            }

            temp.add(new NhTxTemp(dt, dateIso, withdraw, deposit, balance, description));
            matched++;

            i = segEnd - 1;
        }

        if (matched == 0) {
            StringBuilder sb = new StringBuilder();
            int appended = 0;
            for (String rawLine : lines) {
                String line = rawLine == null ? "" : rawLine.trim();
                if (line.isBlank()) continue;
                sb.append(line).append(" | ");
                appended++;
                if (appended >= 20) break;
            }
            log.info("[import-preview][pdf] nh-parser no-match sampleLines={}", sb.toString());
        }

        log.info("[import-preview][pdf] nh-parser matchedRows={}", matched);

        if (temp.isEmpty()) {
            return rows;
        }

        // NH PDF는 보통 최신 거래가 위에 오는 형태(내림차순)이고,
        // '거래후잔액'은 해당 거래가 처리된 후의 잔액이므로
        // 각 행의 거래금액은 (현재행 잔액 - 다음행(더 과거) 잔액)으로 계산된다.
        for (int i = 0; i < temp.size(); i++) {
            NhTxTemp cur = temp.get(i);
            double amount;

            if (i + 1 < temp.size()) {
                NhTxTemp nextOlder = temp.get(i + 1);
                amount = (cur.balance != null && nextOlder.balance != null)
                        ? (cur.balance - nextOlder.balance)
                        : 0.0;
            } else {
                // 마지막(가장 과거) 행은 다음 잔액이 없으므로 표에 있는 출금/입금 컬럼을 최대한 사용
                if (cur.deposit != null && cur.deposit != 0.0) {
                    amount = cur.deposit;
                } else if (cur.withdraw != null && cur.withdraw != 0.0) {
                    amount = -Math.abs(cur.withdraw);
                } else {
                    amount = 0.0;
                }
            }

            rows.add(new ImportPreviewRow(cur.dateIso, cur.description, amount, ""));
        }

        return rows;
    }

    private static boolean isNhAmountToken(String token) {
        if (token == null) return false;
        String t = token.trim();
        if (t.isEmpty()) return false;
        return NH_AMOUNT_TOKEN.matcher(t).matches();
    }

    private static Double parseNhAmountToken(String token) {
        if (token == null) return null;
        String t = token.trim();
        if (t.endsWith("원")) {
            t = t.substring(0, t.length() - 1);
        }
        t = t.replace(",", "");
        if (t.isEmpty()) return null;
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String extractNhDescription(List<String> tokens, int start, int end) {
        if (tokens == null || start < 0 || end <= start || start >= tokens.size()) {
            return "";
        }

        int to = Math.min(end, tokens.size());
        List<String> picked = new ArrayList<>();
        for (int i = start; i < to; i++) {
            String tok = tokens.get(i);
            if (tok == null) continue;
            String t = tok.trim();
            if (t.isEmpty()) continue;

            // 농협 PDF 표에서 잔액 이후에는 [거래내용][거래기록사항][거래점][거래메모] 순으로 이어지는데
            // 프리뷰 내용은 보통 거래내용만 있으면 충분하므로, 경계로 보이는 토큰에서 끊는다.
            if (isNhDescBoundaryToken(t)) {
                break;
            }

            picked.add(t);

            // 너무 길어지면(안내문/면책 조항 등) 중간에서 컷
            if (picked.size() >= 12) {
                break;
            }
        }

        // trailing numeric noise 제거(혹시 남았으면)
        while (!picked.isEmpty() && picked.get(picked.size() - 1).matches("^\\d+$")) {
            picked.remove(picked.size() - 1);
        }

        return String.join(" ", picked).trim();
    }

    private static boolean isNhDescBoundaryToken(String token) {
        if (token == null) return true;
        String t = token.trim();
        if (t.isEmpty()) return true;

        // 거래점 컬럼에 자주 등장
        if (t.equals("농협") || t.equalsIgnoreCase("NH") || t.equalsIgnoreCase("NHBank")) {
            return true;
        }
        // 거래점 코드/순번 등
        if (t.matches("^\\d{6}$") || t.matches("^\\d+$")) {
            return true;
        }
        // 긴 안내문이 시작되는 토큰들
        if (t.contains("고객") || t.contains("법적") || t.contains("효력") || t.contains("감사")) {
            return true;
        }
        return false;
    }

    private List<PdfTxBlock> groupPdfTxBlocks(String text) {
        List<PdfTxBlock> blocks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return blocks;
        }

        String[] lines = text.split("\\R");
        String currentDateIso = null;
        StringBuilder currentBlock = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank()) {
                continue;
            }

            // 토스 PDF의 발급/진위확인/페이지/URL 노이즈 라인은 거래 블록에 합쳐지지 않도록 스킵
            if (isTossNoiseLine(line)) {
                continue;
            }

            Matcher tx = TX_LINE.matcher(line);
            if (tx.matches()) {
                if (currentDateIso != null) {
                    blocks.add(new PdfTxBlock(currentDateIso, currentBlock.toString()));
                }

                currentDateIso = normalizeDate(tx.group(1));
                currentBlock.setLength(0);
                currentBlock.append(tx.group(2));
            } else {
                if (currentDateIso != null) {
                    currentBlock.append(' ').append(line);
                }
            }
        }

        if (currentDateIso != null) {
            blocks.add(new PdfTxBlock(currentDateIso, currentBlock.toString()));
        }

        return blocks;
    }

    private ImportPreviewRow buildRowFromKbBlock(String dateIso, String block) {
        if (dateIso == null || block == null) {
            return null;
        }
        if (!isValidIsoDate(dateIso)) {
            return null;
        }

        String normalized = block.replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) {
            return null;
        }

        String[] tokens = normalized.split(" ");
        int amountIdx = findAmountTripleStart(tokens);
        if (amountIdx < 0) {
            return null;
        }

        Double withdraw = parseAmountToken(tokens[amountIdx]);
        Double deposit = parseAmountToken(tokens[amountIdx + 1]);
        if (withdraw == null || deposit == null) {
            return null;
        }

        double amount = (deposit != 0.0) ? deposit : -withdraw;
        String description = String.join(" ", java.util.Arrays.copyOfRange(tokens, 0, amountIdx)).trim();
        if (description.isBlank()) {
            description = "거래내역";
        }
        return new ImportPreviewRow(dateIso, description, amount, "");
    }

    private static String sanitizeTossBlock(String block) {
        if (block == null) {
            return "";
        }

        String s = block;

        // 링크/안내문 제거
        s = URL_TOKEN.matcher(s).replaceAll(" ");
        s = s.replace("tossbank.com", " ");
        s = s.replace("verify-document", " ");
        s = s.replace("발급번호", " ");
        s = s.replace("발급일자", " ");
        s = s.replace("고객센터", " ");
        s = s.replace("진위확인", " ");
        s = s.replaceAll("\\b\\d+\\s*/\\s*\\d+\\b", " ");

        // 공백이 사라져 붙는 케이스 정규화
        // 11:36:16출금-70,000 -> 11:36:16 출금 -70,000
        // 1,243,520경찰청_장원영 -> 1,243,520 경찰청_장원영
        s = s.replaceAll("(?<=[0-9:])(?=[가-힣A-Za-z_])", " ");
        s = s.replaceAll("(?<=[가-힣A-Za-z_])(?=[+-]\\d)", " ");
        s = s.replaceAll("(?<=\\d)(?=[가-힣A-Za-z_])", " ");

        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private static Double chooseTossTransactionAmount(List<String> amountTokens) {
        if (amountTokens == null || amountTokens.isEmpty()) {
            return null;
        }

        // 토스: [거래종류] [거래금액] [잔액] ... 이므로 첫 번째 금액이 거래금액
        Double first = parseAmountToken(amountTokens.get(0));
        if (first != null) {
            return first;
        }

        return parseAmountToken(amountTokens.get(amountTokens.size() - 1));
    }

    private static boolean isTossNoiseLine(String line) {
        if (line == null) {
            return true;
        }
        String s = line.trim();
        if (s.isBlank()) {
            return true;
        }
        if (s.matches("\\d+\\s*/\\s*\\d+")) {
            return true;
        }
        if (s.contains("발급일자") || s.contains("발급번호") || s.contains("진위확인") || s.contains("고객센터")) {
            return true;
        }
        if (s.contains("http://") || s.contains("https://") || s.contains("tossbank.com") || s.contains("verify-document")) {
            return true;
        }
        return false;
    }

    private ImportPreviewRow buildRowFromTossBlock(String dateIso, String block) {
        if (dateIso == null || block == null) {
            return null;
        }
        if (!isValidIsoDate(dateIso)) {
            return null;
        }

        String normalized = sanitizeTossBlock(block);
        if (normalized.isBlank()) {
            return null;
        }

        String[] tokens = normalized.split(" ");
        List<String> amountTokens = new ArrayList<>();
        for (String t : tokens) {
            if (isAmountToken(t)) {
                amountTokens.add(t);
            }
        }

        if (amountTokens.isEmpty()) {
            return null;
        }

        Double amount = chooseTossTransactionAmount(amountTokens);
        if (amount == null) {
            return null;
        }

        int firstAmountIdx = -1;
        for (int i = 0; i < tokens.length; i++) {
            if (isAmountToken(tokens[i])) {
                firstAmountIdx = i;
                break;
            }
        }
        if (firstAmountIdx < 0) {
            return null;
        }

        String type = String.join(" ", java.util.Arrays.copyOfRange(tokens, 0, firstAmountIdx)).trim();
        if (type.isBlank()) {
            type = "거래";
        }

        // 토스 PDF에서 '-' 부호가 누락되는 케이스 보정
        // 결제/출금/이체 등은 지출(음수), 입금/이자입금 등은 수입(양수)
        String typeNoSpace = type.replace(" ", "");
        boolean shouldBeNegative = typeNoSpace.contains("체크카드결제")
                || typeNoSpace.contains("결제")
                || typeNoSpace.contains("출금")
                || typeNoSpace.contains("오픈뱅킹")
                || typeNoSpace.contains("CMS출금")
                || typeNoSpace.contains("펌뱅킹출금");
        boolean shouldBePositive = typeNoSpace.contains("입금") && !typeNoSpace.contains("출금");

        if (shouldBeNegative && amount > 0) {
            amount = -amount;
        }
        if (shouldBePositive && amount < 0) {
            amount = Math.abs(amount);
        }

        // 프리뷰 화면에서는 내용에 불필요한 상세(금액/상대방/메모)를 제외하고 거래종류만 표시
        String description = type;

        int memoStartIdx = firstAmountIdx + 1;
        // 보통 토스는 [거래금액][잔액] 순서라서, 두 번째 금액 토큰(잔액)을 memo에서 제외
        if (memoStartIdx < tokens.length && isAmountToken(tokens[memoStartIdx])) {
            memoStartIdx++;
        }
        String memo = (memoStartIdx < tokens.length)
                ? String.join(" ", java.util.Arrays.copyOfRange(tokens, memoStartIdx, tokens.length)).trim()
                : "";

        String category = classifyTossCategory(type, memo);

        return new ImportPreviewRow(dateIso, description, amount, category);
    }

    private static String classifyTossCategory(String type, String memo) {
        String t = type == null ? "" : type.replace(" ", "");
        String m = memo == null ? "" : memo.toLowerCase();

        // 우선 거래종류로 빠르게 분류 가능한 케이스
        if (t.contains("체크카드결제") || t.contains("결제")) {
            return "카드결제";
        }
        if (t.contains("이체") || t.contains("송금")) {
            return "이체/송금";
        }

        // 메모/상대방 키워드 기반 분류
        if (m.contains("카카오") || m.contains("택시") || m.contains("tmap")
                || m.contains("버스") || m.contains("지하철") || m.contains("주차") || m.contains("고속") || m.contains("도로")) {
            return "교통";
        }
        if (m.contains("배달") || m.contains("요기") || m.contains("쿠팡") || m.contains("마켓")
                || m.contains("마트") || m.contains("편의점") || m.contains("식사") || m.contains("음식")
                || m.contains("커피") || m.contains("카페")) {
            return "생활비";
        }
        if (m.contains("세금") || m.contains("공과금") || m.contains("관리비") || m.contains("전기")
                || m.contains("가스") || m.contains("수도") || m.contains("통신") || m.contains("인터넷")
                || m.contains("보험") || m.contains("납부") || m.contains("청구")
                || m.contains("경찰청") || m.contains("세무서") || m.contains("법원")) {
            return "공과금";
        }

        // 사람 이름/기관 등(한글이 많은 상대방명) + 출금/이체 계열이면 송금으로 분류
        if ((t.contains("출금") || t.contains("이체") || t.contains("송금")) && memo != null && memo.matches(".*[가-힣]{2,}.*")) {
            return "이체/송금";
        }

        return "기타";
    }

    private static int findAmountTripleStart(String[] tokens) {
        if (tokens == null || tokens.length < 3) {
            return -1;
        }
        for (int i = 0; i + 2 < tokens.length; i++) {
            if (isAmountToken(tokens[i]) && isAmountToken(tokens[i + 1]) && isAmountToken(tokens[i + 2])) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isAmountToken(String token) {
        if (token == null) {
            return false;
        }
        String t = token.trim();
        if (t.isEmpty()) {
            return false;
        }
        return AMOUNT_TOKEN.matcher(t).matches();
    }

    private static Double parseAmountToken(String token) {
        if (token == null) {
            return null;
        }
        String t = token.trim().replace(",", "");
        if (t.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizeDate(String dateStr) {
        if (dateStr == null) {
            return null;
        }
        // 다양한 날짜 형식을 YYYY-MM-DD로 변환
        String normalized = dateStr.replaceAll("[/.]", "-");
        String[] parts = normalized.split("-");
        if (parts.length != 3) {
            return null;
        }
        try {
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            if (year < 1900 || year > 2100 || month < 1 || month > 12 || day < 1 || day > 31) {
                return null;
            }
            return String.format("%04d-%02d-%02d", year, month, day);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<ImportPreviewRow> parsePdf(MultipartFile file, String provider, String pdfPassword) throws Exception {
        List<ImportPreviewRow> rows = new ArrayList<>();

        try (InputStream in = file.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(bytes),
                    pdfPassword == null ? "" : pdfPassword)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(doc);

                String p = provider == null ? "" : provider.trim();

                if (p.isBlank()) {
                    ImportPreviewRow row = new ImportPreviewRow(null, null, null, null);
                    row.addError("은행/카드사를 선택해주세요.");
                    rows.add(row);
                    return rows;
                }

                if (p.equalsIgnoreCase("HYUNDAI")) {
                    List<ImportPreviewRow> parsed = parseHyundaiPdf(text);
                    if (!parsed.isEmpty()) {
                        return parsed;
                    }

                    ImportPreviewRow row = new ImportPreviewRow(null, null, null, null);
                    row.addError("선택한 카드(현대카드) 형식의 PDF가 아닙니다. 올바른 카드사를 선택했는지 확인해주세요.");
                    rows.add(row);
                    return rows;
                }

                if (p.equalsIgnoreCase("TOSS")) {
                    List<ImportPreviewRow> parsed = parseTossPdf(text);
                    if (!parsed.isEmpty()) {
                        return parsed;
                    }

                    ImportPreviewRow row = new ImportPreviewRow(null, null, null, null);
                    row.addError("선택한 은행(토스뱅크) 형식의 PDF가 아닙니다. 올바른 은행을 선택했는지 확인해주세요.");
                    rows.add(row);
                    return rows;
                }

                if (p.equalsIgnoreCase("KB")) {
                    List<ImportPreviewRow> parsed = parseKbPdf(text);
                    if (!parsed.isEmpty()) {
                        return parsed;
                    }

                    ImportPreviewRow row = new ImportPreviewRow(null, null, null, null);
                    row.addError("선택한 은행(국민은행) 형식의 PDF가 아닙니다. 올바른 은행을 선택했는지 확인해주세요.");
                    rows.add(row);
                    return rows;
                }

                if (p.equalsIgnoreCase("NH")) {
                    List<ImportPreviewRow> parsed = parseNhPdf(text);
                    if (!parsed.isEmpty()) {
                        return parsed;
                    }

                    ImportPreviewRow row = new ImportPreviewRow(null, null, null, null);
                    row.addError("선택한 은행(농협) 형식의 PDF가 아닙니다. 올바른 은행을 선택했는지 확인해주세요.");
                    rows.add(row);
                    return rows;
                }

                ImportPreviewRow row = new ImportPreviewRow(null, null, null, null);
                row.addError("선택한 은행/카드사와 PDF 파일이 맞지 않습니다. 올바른 은행을 선택했는지 확인해주세요.");
                rows.add(row);
                return rows;
            }
        } catch (InvalidPasswordException e) {
            ImportPreviewRow row = new ImportPreviewRow(null, null, null, null);
            if (pdfPassword == null || pdfPassword.isBlank()) {
                row.addError("PDF에 비밀번호가 걸려있습니다. 업로드 화면에서 비밀번호를 입력해주세요.");
            } else {
                row.addError("PDF 비밀번호가 올바르지 않습니다. 다시 입력해주세요.");
            }
            rows.add(row);
        }

        return rows;
    }

    private static String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return filename.substring(dot).toLowerCase();
    }

    private static boolean isValidIsoDate(String date) {
        try {
            LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static List<ImportPreviewRow> ensureNotEmpty(List<ImportPreviewRow> rows, String message) {
        if (rows != null && !rows.isEmpty()) {
            return rows;
        }
        List<ImportPreviewRow> fallback = new ArrayList<>();
        ImportPreviewRow row = new ImportPreviewRow(null, null, null, null);
        row.addError(message);
        fallback.add(row);
        return fallback;
    }

    private static void logPreview(String parser, List<ImportPreviewRow> rows) {
        int size = rows == null ? 0 : rows.size();
        log.info("[import-preview] parser={} rows={}", parser, size);
        if (rows == null) {
            return;
        }

        int limit = Math.min(5, rows.size());
        for (int i = 0; i < limit; i++) {
            ImportPreviewRow r = rows.get(i);
            log.info(
                    "[import-preview] sample[{}] date={} desc={} amount={} category={} errors={}",
                    i,
                    r.getDate(),
                    r.getDescription(),
                    r.getAmount(),
                    r.getCategory(),
                    r.getErrors()
            );
        }
    }
}
