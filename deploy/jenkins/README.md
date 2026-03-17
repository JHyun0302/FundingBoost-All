# FundingBoost 로컬 Jenkins CI/CD 테스트 가이드

이 문서는 로컬에서 Jenkins를 독립 실행해 아래 흐름을 검증하기 위한 가이드입니다.

- `main` 브랜치 push(GitHub Actions) -> Jenkins 웹훅 호출 -> Jenkins 파이프라인 실행
- 또는 로컬 스크립트로 Jenkins 웹훅 수동 호출

## 0) 비밀정보 관리 규칙

- `deploy/jenkins/.env`는 민감정보 파일이므로 Git에 커밋하지 않습니다.
- 저장소에는 `deploy/jenkins/.env.example`만 유지합니다.
- 계정/비밀번호는 개인 비밀 저장소(예: Obsidian 개인 Vault, 1Password 등)에만 기록합니다.
- 아래 계정값은 **로컬 테스트용 기본값**이며 운영환경 재사용 금지입니다.

### 로컬 Jenkins 기록값(요청 반영)

- 노트 제목: `Local Jenkins (FundingBoost)`
- URL: `http://localhost:8081`
- username: `fundingboost`
- password: `fundingboost`
- email: `notion030299@gmail.com`
- job: `fundingboost-local-cd`
- token: `local-fundingboost-token`

## 1) Jenkins 실행

```bash
cp deploy/jenkins/.env.example deploy/jenkins/.env
docker compose --env-file deploy/jenkins/.env -f docker-compose.jenkins.yml up -d --build
```

접속 URL:

- `http://localhost:8081`

최초 잠금 해제 비밀번호 확인:

```bash
docker exec fundingboost-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

## 2) 파이프라인 Job 생성(최초 1회)

Jenkins에서 `Pipeline` 타입 Job 생성:

- Job 이름: `fundingboost-local-cd`
- `This project is parameterized` 활성화 후 String 파라미터 추가:
  - `ref`
  - `sha`
  - `repository`
  - `actor`
- `Build Triggers`에서 `Trigger builds remotely (e.g., from scripts)` 체크
  - Token: `local-fundingboost-token` (원하면 변경 가능)
- 파이프라인 정의:
  - 가장 간단한 방식은 `Pipeline script` 선택 후 아래 파일 내용 붙여넣기
  - `deploy/jenkins/jobs/fundingboost-local-cd.Jenkinsfile`

## 3) 로컬 웹훅 테스트 (GitHub 없이 동작 검증)

```bash
chmod +x deploy/jenkins/scripts/*.sh
bash deploy/jenkins/scripts/trigger-local-webhook.sh
```

Jenkins에서 `fundingboost-local-cd` 빌드가 생성되면 정상입니다.

## 4) GitHub Actions -> Jenkins 연동 테스트

워크플로우 파일:

- `.github/workflows/jenkins-webhook-dispatch.yml`

GitHub Repository Secrets 설정:

- `JENKINS_WEBHOOK_BASE_URL`
  - 예: `https://<jenkins-host>/buildByToken/buildWithParameters`
- `JENKINS_JOB_NAME`
  - 예: `fundingboost-local-cd`
- `JENKINS_WEBHOOK_TOKEN`
  - Jenkins Job의 remote trigger token과 동일해야 함

`main` push 시 GitHub Actions가 아래 파라미터를 Jenkins로 전송합니다.

- `job`
- `token`
- `ref`
- `sha`
- `repository`
- `actor`

주의:

- Jenkins URL이 `localhost`면 GitHub에서 접근할 수 없습니다.
- 실제 E2E 검증은 Jenkins를 외부에서 접근 가능한 URL(터널/공인 도메인)로 노출해야 합니다.

## 5) 실제 배포 연결

현재 기본 동작은 안전한 드라이런입니다.

- `deploy/jenkins/scripts/local-deploy.sh`의 기본값: `LOCAL_DEPLOY_DRY_RUN=true`

실제 OCI 배포를 연결하려면:

- `deploy/jenkins/scripts/deploy-to-oci.sh`에 실배포 명령 구현
- Jenkins Job 환경변수에서 `LOCAL_DEPLOY_DRY_RUN=false` 설정
