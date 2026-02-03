# Expense Tracker App (가계부)

Spring Boot + MySQL 기반의 개인 가계부 웹 앱입니다.

은행/카드사 PDF 거래내역을 업로드해 미리보기/수정 후 확정 저장하고,
월별 대시보드 분석과 함께 고정지출/고정수입/투자자산/대출(월별 상환이력)까지 한 화면에서 관리합니다.

## 주요 기능

- **인증(세션 기반)**
  - 로그인/회원가입/로그아웃
  - 사용자별 데이터 격리(본인 데이터만 조회/변경)
- **PDF 업로드/미리보기/확정 저장**
  - PDF 파싱 → 미리보기 테이블 표시
  - 미리보기에서 행 수정(내용/금액/카테고리)
  - 확정 저장 시 DB에 반영되며 월 단위로 잠금/해제 처리
- **대시보드**
  - 월별 수입/지출, 카테고리별 분석, (전체 선택 시) 제공처별 지출
- **고정지출/고정수입 관리**
  - 월별 확인/잠금 흐름 포함
- **투자자산 관리**
  - 자산 CRUD 및 요약 표시
- **대출현황(월별 상환이력 기반)**
  - 대출 마스터(현재 남은 원금) + 월별 상환 이력 분리
  - 월(YYYY-MM) 선택 → 해당 월 상환 여부 조회 → "이번달 상환" 버튼 활성/비활성
  - 중복 대출 항목 정리(동일 기관/대출명/종류는 1개만 남기기)

## 지원 PDF 형식(Provider)

- `TOSS` (토스)
- `KB` (국민)
- `NH` (농협)
- `HYUNDAI` (현대카드)

## 기술 스택

- **Backend**: Spring Boot, Spring Web, Spring Data JPA
- **Auth**: HttpSession 기반 인증 + BCrypt(spring-security-crypto)
- **DB**: MySQL
- **PDF Parsing**: Apache PDFBox
- **Frontend**: Thymeleaf + Vanilla JS + Chart.js

## 실행 방법 (로컬)

### 1) MySQL 준비

```sql
CREATE DATABASE expense_tracker;
```

`src/main/resources/application.properties`의 DB 접속 정보를 환경에 맞게 설정합니다.

### 2) 실행

```bash
./mvnw spring-boot:run
```

접속: http://localhost:8080

## 실행 방법 (Docker)

이 레포에는 `docker-compose.yml`이 포함되어 있습니다.

- 자세한 명령어/운영 팁: `DOCKER_COMMANDS.md`

## DB 반영/스키마

- 전체 스키마 문서: `DATABASE_SCHEMA.md`
- 대출 월별 상환이력 테이블(`loan_payment_history`) 생성/제약 반영 SQL: `DOCKER_COMMANDS.md` 참고

## 프로젝트 구조(주요)

```text
src/main/java/com/example/Expense_Tracker_App/
  controller/
  service/
  repository/
  entity/
src/main/resources/
  templates/index.html
  static/script.js
  static/styles.css
  application.properties
```

## Troubleshooting

- CSS/JS 변경이 반영되지 않으면 브라우저 강력 새로고침(Ctrl+F5)

## License

MIT
