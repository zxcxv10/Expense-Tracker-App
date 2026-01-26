# Expense Tracker App (가계부)

Spring Boot + MySQL 기반의 **가계부/거래내역 관리** 웹 애플리케이션입니다.

은행/카드사에서 내려받은 **PDF 거래내역**을 업로드하여 미리보기 후 확정 저장하고, 확정된 데이터 기반으로 **대시보드(월별/카테고리별 분석)** 를 제공합니다.

## 주요 기능

- **인증(세션 기반)**
  - 로그인/회원가입/로그아웃
  - 로그인 사용자별 데이터 격리(본인 데이터만 조회)
- **PDF 업로드/미리보기/확정 저장**
  - PDF 업로드 후 거래내역 파싱 → 미리보기 테이블 표시
  - 미리보기에서 행 수정(내용/금액/카테고리)
  - "확정 저장" 시 DB에 저장되며 해당 월은 잠금(재저장/수정 불가)
  - 비밀번호(PDF 암호) 걸린 파일은 비밀번호 입력 후 재시도
- **대시보드**
  - 확정된 데이터(confirmed=Y) 기반
  - 월별 수입/지출, 카테고리별 수입/지출, (전체 선택 시) 은행/카드사별 지출
  - 로그인 후/새로고침 후 자동 로드

## 지원 PDF 형식(Provider)

- `TOSS` (토스)
- `KB` (국민)
- `NH` (농협)
- `HYUNDAI` (현대카드)

> 주의: 확정 저장은 **한 번에 한 달(년/월) 데이터만** 허용합니다. (다른 월이 섞이면 오류)

## 기술 스택

- **Backend**: Spring Boot 3.2, Spring Web, Spring Data JPA
- **Auth**: HttpSession 기반 인증 + BCrypt(spring-security-crypto)
- **DB**: MySQL
- **PDF Parsing**: Apache PDFBox
- **Frontend**: Thymeleaf(서빙) + Vanilla JS + Chart.js

## 실행 방법 (로컬)

### 1) MySQL 준비

MySQL에 DB 생성:

```sql
CREATE DATABASE expense_tracker;
```

`src/main/resources/application.properties`는 DB 접속정보를 **환경변수**로 받도록 구성되어 있습니다.

로컬에서는 `.env.example`을 복사해 `.env`로 만든 뒤(커밋 금지), 환경변수를 주입해서 실행하세요.

예시(Windows PowerShell):

```powershell
Copy-Item .env.example .env
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/expense_tracker?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="your_password"
```

예시(Unix/macOS):

```bash
cp .env.example .env
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/expense_tracker?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="your_password"
```

참고: Docker/Compose 사용 시에는 `--env-file .env` 형태로 주입하는 방식을 권장합니다.

기존 `application.properties` 예시(기본값):

```properties
server.port=8080

spring.datasource.url=jdbc:mysql://localhost:3306/expense_tracker?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
spring.datasource.username=
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}
```

### 2) 실행

```bash
./mvnw spring-boot:run
```

접속:

- http://localhost:8080

## Docker 배포(권장: App + MySQL 분리)

현재 레포에는 Dockerfile/compose 파일이 포함되어 있지 않습니다.

권장 구성:

- `app`: Spring Boot jar 실행
- `db`: mysql:8.0

필요 시 아래 환경변수를 app 컨테이너에 주입하세요:

- `SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/expense_tracker?...`
- `SPRING_DATASOURCE_USERNAME=root`
- `SPRING_DATASOURCE_PASSWORD=...`

## 인증/권한 모델

- `/api/import/**`, `/api/dashboard/**`는 로그인 필요(401)
- 미로그인 상태에서 호출 시 로그인 모달이 열리고 안내 메시지 표시
- 저장/조회 데이터는 `createdBy`(로그인 username) 기준으로 필터링

## 프로젝트 구조(주요)

```text
src/main/java/com/example/Expense_Tracker_App/
  controller/        # Auth/Import/Dashboard API
  service/           # PDF 파싱, 확정 저장, 대시보드 집계
  repository/        # JPA Repository
  entity/            # Transaction, User
src/main/resources/
  templates/index.html
  static/script.js
  static/styles.css
  application.properties
```

## Troubleshooting

- **PDF 비밀번호 입력창이 안 뜸**
  - 비밀번호 오류 응답(데이터 row 없음)에도 입력창이 뜨도록 처리되어 있습니다.
  - 브라우저 캐시 이슈가 있으면 강력 새로고침(Ctrl+F5) 후 재시도하세요.

## License

MIT
