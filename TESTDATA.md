# FundingBoost 테스트 데이터 가이드

## 개요

Docker로 서버를 처음 올릴 때 `fundingboost` 스키마가 비어 있으면, 서버가 QA 테스트 데이터를 1회 자동 생성합니다.

- 대상 환경
  - 로컬: `docker-compose.local.yml`
  - 운영(Back VM): `docker-compose.back.yml`
- 전제 조건
  - `TESTDATA_BOOTSTRAP_ENABLED=true`
  - `item` 스키마의 `item` 테이블에 크롤러 데이터가 존재
  - 카테고리 5개 이상, 각 카테고리당 아이템 2개 이상

서버는 위 조건이 만족될 때까지 10초 간격으로 재시도합니다.

## 자동 생성 계정

비밀번호는 모두 동일합니다.

- `qa1@fundingboost.test / Test1234!` : 마리오
- `qa2@fundingboost.test / Test1234!` : 루이지
- `qa3@fundingboost.test / Test1234!` : 피치공주
- `qa4@fundingboost.test / Test1234!` : 키노피오
- `qa5@fundingboost.test / Test1234!` : 요시

기본값:

- 각 사용자 포인트: `50,000`
- 성별:
  - 마리오: `MAN`
  - 루이지: `MAN`
  - 피치공주: `WOMAN`
  - 키노피오: `MAN`
  - 요시: `WOMAN`
- 프로필 이미지:
  - 마리오: `/test-members/mario.png`
  - 루이지: `/test-members/luigi.png`
  - 피치공주: `/test-members/princess-peach.png`
  - 키노피오: `/test-members/toad.png`
  - 요시: `/test-members/yoshi.png`

원본 PNG 보관 위치:

- `assets/test-members/source-png`

배포용 정적 이미지 위치:

- `FundingBoost-Client/fundingboost/public/test-members`

## 자동 생성 데이터

### 1. 친구 관계

5명 모두 서로 친구입니다.

- 완전 연결(Complete Graph)
- 관계 테이블에는 양방향으로 저장됩니다.

즉, 5명 기준 총 `20`개의 `relationship` 레코드가 생성됩니다.

### 2. 북마크

각 사용자마다:

- 카테고리 5개
- 카테고리당 랜덤 2개
- 총 10개 아이템을 북마크

전체 기준 총 `50`개의 `bookmark` 레코드가 생성됩니다.

### 3. 펀딩

각 사용자마다:

- 랜덤 아이템 1개로 펀딩 1개 생성
- 각 펀딩에는 `funding_item` 1개 연결

전체 기준:

- `funding` 5개
- `funding_item` 5개

### 4. 펀딩 참여자(Contributor)

각 펀딩마다:

- 본인을 제외한 나머지 4명이 참여
- 참여 금액은 테스트용 고정 패턴으로 들어갑니다.

전체 기준 총 `20`개의 `contributor` 레코드가 생성됩니다.

### 5. 주문 데이터

`많이 구매한` 랭킹까지 바로 테스트할 수 있도록 주문도 함께 생성됩니다.

각 사용자마다:

- 배송지 1개
- 주문 1개
- 주문 아이템 1개

전체 기준:

- `delivery` 5개
- `orders` 5개
- `order_item` 5개

## 로컬 실행

완전 초기화 후 테스트 데이터를 다시 넣으려면:

```bash
docker compose -f docker-compose.local.yml down -v --remove-orphans
docker compose -f docker-compose.local.yml up -d --build
```

확인 순서:

1. 크롤러가 `item` 스키마에 아이템 적재
2. 서버가 10초 간격으로 조건 확인
3. 테스트 계정 및 관계/북마크/펀딩/주문 자동 생성

## 운영(Back VM) 실행

완전 초기화 후 운영 테스트 데이터를 다시 넣으려면:

```bash
docker compose --env-file deploy/oci/env/back.env -f docker-compose.back.yml down -v --remove-orphans
docker compose --env-file deploy/oci/env/back.env -f docker-compose.back.yml up -d --build
```

운영도 동일하게 `item` 스키마 준비 후 서버가 자동 시드합니다.

## 확인용 SQL

```sql
SELECT COUNT(*) AS member_count FROM fundingboost.member;
SELECT COUNT(*) AS relationship_count FROM fundingboost.relationship;
SELECT COUNT(*) AS bookmark_count FROM fundingboost.bookmark;
SELECT COUNT(*) AS funding_count FROM fundingboost.funding;
SELECT COUNT(*) AS funding_item_count FROM fundingboost.funding_item;
SELECT COUNT(*) AS contributor_count FROM fundingboost.contributor;
SELECT COUNT(*) AS delivery_count FROM fundingboost.delivery;
SELECT COUNT(*) AS order_count FROM fundingboost.orders;
SELECT COUNT(*) AS order_item_count FROM fundingboost.order_item;
```

정상 기대값:

- `member_count = 5`
- `relationship_count = 20`
- `bookmark_count = 50`
- `funding_count = 5`
- `funding_item_count = 5`
- `contributor_count = 20`
- `delivery_count = 5`
- `order_count = 5`
- `order_item_count = 5`

추가 확인:

- `SELECT email, profile_img_url FROM fundingboost.member WHERE email LIKE 'qa%@fundingboost.test';`

## 주의사항

- 이미 `qa1@fundingboost.test`가 있으면 중복 생성하지 않습니다.
- 기존 일반 회원이 있어도 QA 계정이 없으면 테스트 계정 5종은 추가 생성됩니다.
- 아이템 카테고리 조건이 충족되지 않으면 서버가 계속 대기합니다.
- 다시 생성하려면 반드시 `down -v`로 볼륨까지 비우는 것이 가장 안전합니다.
