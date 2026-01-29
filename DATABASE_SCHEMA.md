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
