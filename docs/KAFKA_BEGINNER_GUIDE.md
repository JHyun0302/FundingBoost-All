# Kafka 완전 입문 가이드 (FundingBoost 기준)

## Kafka를 한 줄로
Kafka는 "서비스끼리 말을 전달해주는 대용량 메시지 버스"다.

## 아주 쉬운 비유
- `Topic` = 단체 채팅방
- `Producer` = 채팅 보내는 사람
- `Consumer` = 채팅 읽는 사람
- `Message(Event)` = 채팅 메시지
- `Offset` = 채팅방에서 어디까지 읽었는지 번호

즉, A 서비스가 이벤트를 Topic에 보내면, B/C/D 서비스가 각자 필요할 때 읽는다.

## 왜 Kafka를 쓰나?
- 서비스끼리 직접 붙어 있지 않아도 된다 (느슨한 결합)
- 한 서비스가 잠깐 죽어도, 이벤트는 Kafka에 남아 나중에 읽을 수 있다
- 같은 이벤트를 여러 서비스가 동시에 활용 가능

## 이번 프로젝트에서의 흐름
1. 비즈니스 트랜잭션 성공
2. 같은 트랜잭션 안에서 `outbox_event`에 이벤트 저장
3. 별도 릴레이가 outbox를 읽어 Kafka로 발행
4. 소비 서비스가 Topic에서 읽어 후속 처리

이 방식 덕분에 "DB는 반영됐는데 이벤트 발행만 실패"하는 문제를 줄일 수 있다.

## 지금 추가된 대표 이벤트
- `fundingboost.funding.contribution.created.v1`
- `fundingboost.commerce.order.paid.v1`

## 로컬에서 확인하는 가장 쉬운 방법
1. `docker compose -f docker-compose.local.yml up -d`
2. 브라우저에서 `http://localhost:8085` 접속 (Kafka UI)
3. Topic 생성/메시지 유입 확인
4. DB에서 outbox 확인

```sql
SELECT status, COUNT(*) FROM fundingboost.outbox_event GROUP BY status;
SELECT * FROM fundingboost.outbox_event ORDER BY outbox_event_id DESC LIMIT 20;
```

## 자주 하는 실수
- Kafka만 쓰고 outbox를 안 쓰는 경우
  - DB 성공 + Kafka 실패 시 데이터 정합성 깨짐
- 이벤트 스키마 버전 없이 운영하는 경우
  - 나중에 Consumer 깨질 가능성 큼

## 권장 습관
- 이벤트 이름에 버전 붙이기 (`*.v1`)
- payload는 "사실(fact)" 중심으로 보내기
- 재시도/데드레터 정책을 초기에 설계하기

