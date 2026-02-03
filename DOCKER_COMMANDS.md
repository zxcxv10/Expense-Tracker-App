# Docker 관련 명령어 정리

## 기본 실행/상태 확인
```bash
# 실행 (백그라운드)
docker compose up -d

# 종료
docker compose down

# 상태 확인
docker compose ps

# 로그 확인 (전체)
docker compose logs

# 로그 확인 (특정 서비스)
docker compose logs app
docker compose logs mysql
```

## 빌드/재빌드
```bash
# 변경 사항 반영 빌드 후 실행
Docker compose up -d --build

# 캐시 없이 완전 재빌드
Docker compose build --no-cache

# 캐시 없이 재빌드 + 실행
Docker compose down
docker compose build --no-cache
docker compose up -d
```

## 문제 해결 체크리스트
```bash
# 컨테이너 상태 확인
docker compose ps

# 앱 로그 확인
docker compose logs app

# DB 로그 확인
docker compose logs mysql
```

## DB 반영 (loan_payment_history)
```sql
CREATE TABLE IF NOT EXISTS loan_payment_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  loan_id BIGINT NOT NULL,
  payment_ym VARCHAR(7) NOT NULL,
  payment_amount DECIMAL(15,2) NOT NULL,
  remaining_after DECIMAL(15,2) NOT NULL,
  created_at DATETIME
);

CREATE INDEX idx_lph_user_ym ON loan_payment_history(username, payment_ym);

-- 같은 달 중복 상환 기록 방지
ALTER TABLE loan_payment_history
  ADD CONSTRAINT uk_lph_user_loan_ym UNIQUE (username, loan_id, payment_ym);
```

## 참고
- 로컬 소스 변경이 즉시 반영되지 않으면 **재빌드가 필요**합니다.
- 브라우저 캐시로 인해 CSS/JS 변경이 안 보일 수 있으니 **강력 새로고침(Ctrl+F5)** 하세요.
