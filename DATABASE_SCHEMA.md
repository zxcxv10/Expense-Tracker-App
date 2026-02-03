# DB 테이블 정의서 (JPA Entity 기준)

이 문서는 `src/main/java/com/example/Expense_Tracker_App/entity`의 **JPA Entity**를 기준으로 정리한 DB 테이블 정의서입니다.

- **주요 특징**
  - 로그인 사용자별 데이터는 `username`(users) / `created_by`(transactions) 기준으로 분리됩니다.
  - 월별 업로드 확정 데이터는 `transactions.confirmed='Y'` 기준으로 대시보드 집계에 반영됩니다.
  - 고정지출/고정수입 자동생성은 `transactions.gen_year`, `transactions.gen_month`와 `fixed_*_id`를 사용해 **월 단위 중복 생성 방지**를 합니다(Unique Constraint).

---

## 1) `users` (사용자)

- **역할**: 세션 기반 인증에서 사용하는 사용자 계정 정보

| 컬럼명 | 타입(예상) | NULL | 설명 |
|---|---|---:|---|
| `id` | BIGINT | N | PK (AUTO_INCREMENT) |
| `username` | VARCHAR(50) | N | 로그인 ID(이름). 앱 내 데이터 격리 키로도 사용 |
| `password` | VARCHAR(255) | N | BCrypt 해시 비밀번호 |
| `role` | VARCHAR(20) | Y | 권한/역할(현재는 단순 문자열) |
| `created_at` | DATETIME | Y | 생성일시 |

---

## 2) `transactions` (거래 내역)

- **역할**: 업로드된 거래 내역 + 고정항목 자동생성 내역까지 **모든 거래 데이터의 중심 테이블**

| 컬럼명 | 타입(예상) | NULL | 설명 |
|---|---|---:|---|
| `id` | BIGINT | N | PK (AUTO_INCREMENT) |
| `provider` | VARCHAR(20) | N | 거래 출처(예: `TOSS`, `KB`, `NH`, `HYUNDAI`, `FIXED_EXPENSE`, `FIXED_INCOME`) |
| `tx_year` | INT | N | 거래 연도 |
| `tx_month` | INT | N | 거래 월 |
| `tx_date` | DATE | N | 거래 일자 |
| `description` | VARCHAR(500) | N | 거래 내용(설명) |
| `tx_type` | VARCHAR(50) | Y | 구분(입금/출금/결제 등) |
| `tx_detail` | VARCHAR(500) | Y | 거래 상세(가맹점/메모 등) |
| `amount` | DECIMAL(12,2) | N | 금액(고정수입은 양수로 저장) |
| `post_balance` | DECIMAL(12,2) | Y | 거래 후 잔액 |
| `category` | VARCHAR(50) | Y | 카테고리 |
| `created_at` | DATETIME | Y | 생성일시 |
| `updated_at` | DATETIME | Y | 수정일시 |
| `created_by` | VARCHAR(50) | Y | 생성 사용자(username) |
| `updated_by` | VARCHAR(50) | Y | 수정 사용자(username) |
| `confirmed` | VARCHAR(1) | Y | 확정 여부 (`Y`/`N`) |
| `confirmed_at` | DATETIME | Y | 확정 일시 |
| `confirmed_by` | VARCHAR(50) | Y | 확정 사용자(username) |
| `fixed_expense_id` | BIGINT | Y | 고정지출에서 생성된 거래일 때 연결되는 ID |
| `fixed_income_id` | BIGINT | Y | 고정수입에서 생성된 거래일 때 연결되는 ID |
| `gen_year` | INT | Y | 고정항목 자동생성 기준 연도(월중복방지 키) |
| `gen_month` | INT | Y | 고정항목 자동생성 기준 월(월중복방지 키) |

### Unique Constraints
- **`uk_tx_fixed_expense_month`**: (`created_by`, `fixed_expense_id`, `gen_year`, `gen_month`)
  - 고정지출 자동생성 시 같은 월에 중복 생성 방지
- **`uk_tx_fixed_income_month`**: (`created_by`, `fixed_income_id`, `gen_year`, `gen_month`)
  - 고정수입 자동생성 시 같은 월에 중복 생성 방지

---

## 3) `fixed_expenses` (고정 지출 마스터)

- **역할**: 매달 반복되는 지출 템플릿(자동생성 시 `transactions`로 복제)

| 컬럼명 | 타입(예상) | NULL | 설명 |
|---|---|---:|---|
| `id` | BIGINT | N | PK (AUTO_INCREMENT) |
| `username` | VARCHAR(50) | N | 소유자(username) |
| `title` | VARCHAR(100) | N | 항목명 |
| `account` | VARCHAR(50) | Y | 통장/카드 |
| `amount` | DECIMAL(12,2) | N | 금액 |
| `category` | VARCHAR(50) | Y | 카테고리 |
| `billing_day` | INT | N | 결제일(1~31) |
| `memo` | VARCHAR(500) | Y | 메모 |
| `status` | VARCHAR(10) | Y | 상태(`ACTIVE`/`PAUSED`) |
| `created_at` | DATETIME | Y | 생성일시 |
| `updated_at` | DATETIME | Y | 수정일시 |

---

## 4) `fixed_incomes` (고정 수입 마스터)

- **역할**: 매달 반복되는 수입 템플릿(자동생성 시 `transactions`로 복제)

| 컬럼명 | 타입(예상) | NULL | 설명 |
|---|---|---:|---|
| `id` | BIGINT | N | PK (AUTO_INCREMENT) |
| `username` | VARCHAR(50) | N | 소유자(username) |
| `title` | VARCHAR(100) | N | 항목명 |
| `account` | VARCHAR(50) | Y | 통장 |
| `amount` | DECIMAL(12,2) | N | 금액 |
| `category` | VARCHAR(50) | Y | 카테고리 |
| `payday` | INT | N | 입금일(1~31) |
| `memo` | VARCHAR(500) | Y | 메모 |
| `status` | VARCHAR(10) | Y | 상태(`ACTIVE`/`PAUSED`) |
| `created_at` | DATETIME | Y | 생성일시 |
| `updated_at` | DATETIME | Y | 수정일시 |

---

## 5) `fixed_expense_auto_settings` (고정지출 자동생성 설정)

- **역할**: 사용자별 고정지출 자동생성(매달 1일) ON/OFF 및 최근 실행 상태 저장

| 컬럼명 | 타입(예상) | NULL | 설명 |
|---|---|---:|---|
| `id` | BIGINT | N | PK (AUTO_INCREMENT) |
| `username` | VARCHAR(50) | N | 사용자(username) |
| `enabled` | BOOLEAN | Y | 자동생성 활성화 여부 |
| `last_run_at` | DATETIME | Y | 마지막 실행 시각 |
| `last_run_message` | VARCHAR(500) | Y | 마지막 실행 메시지 |
| `created_at` | DATETIME | Y | 생성일시 |
| `updated_at` | DATETIME | Y | 수정일시 |

### Unique Constraints
- **`uk_fixed_expense_auto_settings_username`**: (`username`)

---

## 6) `fixed_income_auto_settings` (고정수입 자동생성 설정)

- **역할**: 사용자별 고정수입 자동생성(매달 1일) ON/OFF 및 최근 실행 상태 저장

| 컬럼명 | 타입(예상) | NULL | 설명 |
|---|---|---:|---|
| `id` | BIGINT | N | PK (AUTO_INCREMENT) |
| `username` | VARCHAR(50) | N | 사용자(username) |
| `enabled` | BOOLEAN | Y | 자동생성 활성화 여부 |
| `last_run_at` | DATETIME | Y | 마지막 실행 시각 |
| `last_run_message` | VARCHAR(500) | Y | 마지막 실행 메시지 |
| `created_at` | DATETIME | Y | 생성일시 |
| `updated_at` | DATETIME | Y | 수정일시 |

### Unique Constraints
- **`uk_fixed_income_auto_settings_username`**: (`username`)

---

## 7) `investment_assets` (투자자산)

- **역할**: 투자자산 마스터(계좌/자산 분류/자산명) 및 평가금액/원가/수량 등의 관리

| 컬럼명 | 타입(예상) | NULL | 설명 |
|---|---|---:|---|
| `id` | BIGINT | N | PK (AUTO_INCREMENT) |
| `username` | VARCHAR(50) | N | 소유자(username) |
| `asset_type` | VARCHAR(50) | N | 분류(예: 주식/코인/예적금 등) |
| `account_name` | VARCHAR(100) | Y | 계좌/증권사/거래소 |
| `asset_name` | VARCHAR(100) | N | 자산명 |
| `quantity` | DECIMAL(18,8) | Y | 수량 |
| `avg_buy_price` | DECIMAL(18,8) | Y | 평균 매입가 |
| `evaluated_amount` | DECIMAL(15,2) | N | 보유금액(수동) |
| `cost_amount` | DECIMAL(15,2) | Y | 매입원금(수동) |
| `memo` | VARCHAR(500) | Y | 메모 |
| `created_at` | DATETIME | Y | 생성일시 |
| `updated_at` | DATETIME | Y | 수정일시 |

---

## 8) `loans` (대출 마스터)

- **역할**: 대출 현황의 마스터 테이블(현재 남은 원금/월상환액 등)
- 월별 상환 여부/이력은 `loan_payment_history`로 분리하여 관리합니다.

| 컬럼명 | 타입(예상) | NULL | 설명 |
|---|---|---:|---|
| `id` | BIGINT | N | PK (AUTO_INCREMENT) |
| `username` | VARCHAR(50) | N | 소유자(username) |
| `lender` | VARCHAR(100) | Y | 금융기관 |
| `loan_name` | VARCHAR(120) | N | 대출명 |
| `loan_type` | VARCHAR(50) | Y | 종류(예: 주담대/신용대출 등) |
| `principal_amount` | DECIMAL(15,2) | Y | 대출원금(참고) |
| `remaining_principal` | DECIMAL(15,2) | N | 남은 원금(현황) |
| `interest_rate` | DECIMAL(6,3) | Y | 금리(연 %) |
| `repayment_type` | VARCHAR(50) | Y | 상환 방식 |
| `monthly_payment` | DECIMAL(15,2) | Y | 월 상환액 |
| `last_payment_ym` | VARCHAR(7) | Y | (레거시) 마지막 상환 월(YYYY-MM) |
| `maturity_date` | DATE | Y | 만기일 |
| `memo` | VARCHAR(500) | Y | 메모 |
| `created_at` | DATETIME | Y | 생성일시 |
| `updated_at` | DATETIME | Y | 수정일시 |

---

## 9) `loan_payment_history` (대출 월별 상환 이력)

- **역할**: 월(YYYY-MM) 단위로 "상환 완료" 내역을 기록하는 이력 테이블
- 프론트에서 선택한 월(`YYYY-MM`) 기준으로 상환 여부를 조회하여, 동일 월 중복 상환을 막고 버튼 상태를 제어합니다.

| 컬럼명 | 타입(예상) | NULL | 설명 |
|---|---|---:|---|
| `id` | BIGINT | N | PK (AUTO_INCREMENT) |
| `username` | VARCHAR(50) | N | 사용자(username) |
| `loan_id` | BIGINT | N | 대출 ID(`loans.id`) |
| `payment_ym` | VARCHAR(7) | N | 상환 월(YYYY-MM) |
| `payment_amount` | DECIMAL(15,2) | N | 상환 처리 금액(월상환액) |
| `remaining_after` | DECIMAL(15,2) | N | 상환 처리 후 남은 원금 스냅샷 |
| `created_at` | DATETIME | Y | 생성일시 |

### Unique Constraints
- **`uk_lph_user_loan_ym`**: (`username`, `loan_id`, `payment_ym`)
  - 같은 대출이 같은 월에 중복 상환 기록이 생성되는 것을 방지
