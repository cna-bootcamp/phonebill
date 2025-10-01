# GitHub Actions 백엔드 CI/CD 파이프라인 가이드

## 📋 개요

이 문서는 phonebill 프로젝트의 GitHub Actions 기반 백엔드 CI/CD 파이프라인 구축 가이드입니다.

**주요 기능:**
- ✅ Gradle 기반 빌드 및 단위 테스트
- ✅ SonarQube 코드 품질 분석 및 Quality Gate 검증
- ✅ 환경별(dev/staging/prod) Docker 이미지 빌드 및 푸시
- ✅ Kustomize 기반 환경별 매니페스트 관리 및 자동 배포
- ✅ 롤백 및 수동 배포 지원

---

## 📌 1. 프로젝트 정보

### 시스템 및 서비스 정보
- **시스템명**: `phonebill`
- **서비스 목록**:
  - `api-gateway` (API Gateway)
  - `user-service` (사용자 인증 및 관리)
  - `bill-service` (요금 조회)
  - `product-service` (상품 변경)
  - `kos-mock` (KOS 연동 Mock)

### 기술 스택
- **JDK**: 21
- **빌드 도구**: Gradle
- **컨테이너**: Docker
- **오케스트레이션**: Kubernetes (AKS)
- **매니페스트 관리**: Kustomize

### 인프라 정보
- **Azure Container Registry**: `acrdigitalgarage01`
- **리소스 그룹**: `rg-digitalgarage-01`
- **AKS 클러스터**: `aks-digitalgarage-01`
- **Namespace**: `phonebill-dg0500`

---

## 🔧 2. 사전 준비사항

### 2.1 Azure 인증 정보 획득

#### Azure Service Principal 생성

```bash
# Azure CLI로 Service Principal 생성
az ad sp create-for-rbac \
  --name "github-actions-phonebill" \
  --role contributor \
  --scopes /subscriptions/{구독ID}/resourceGroups/rg-digitalgarage-01 \
  --json-auth

# 출력 예시 (이 JSON을 AZURE_CREDENTIALS Secret으로 등록)
{
  "clientId": "5e4b5b41-7208-48b7-b821-d6d5acf50ecf",
  "clientSecret": "ldu8Q~GQEzFYU.dJX7_QsahR7n7C2xqkIM6hqbV8",
  "subscriptionId": "2513dd36-7978-48e3-9a7c-b221d4874f66",
  "tenantId": "4f0a3bfd-1156-4cce-8dc2-a049a13dba23"
}
```

#### ACR Credentials 획득

```bash
# ACR 패스워드 확인
az acr credential show --name acrdigitalgarage01

# 출력 결과에서 username과 password 확인
```

### 2.2 SonarQube 설정

#### SONAR_HOST_URL 확인

```bash
# SonarQube 서비스 External IP 확인
kubectl get svc -n sonarqube

# 출력 예시:
# NAME                TYPE           EXTERNAL-IP      PORT(S)
# sonarqube-sonarqube LoadBalancer   20.249.187.69    9000:30234/TCP

# SONAR_HOST_URL: http://20.249.187.69
```

#### SONAR_TOKEN 생성

1. SonarQube에 로그인 (http://{EXTERNAL_IP})
2. 우측 상단 **Administrator** > **My Account** 클릭
3. **Security** 탭 선택
4. **Generate Tokens**에서 새 토큰 생성
5. 토큰 값 복사 (한 번만 표시됨)

### 2.3 Docker Hub 인증 (Rate Limit 방지)

1. Docker Hub (https://hub.docker.com) 로그인
2. 우측 상단 프로필 아이콘 > **Account Settings**
3. 좌측 메뉴 **Personal Access Tokens** 클릭
4. **Generate New Token** 버튼으로 토큰 생성
5. Token 값 복사

---

## 🔐 3. GitHub Repository 설정

### 3.1 Repository Secrets 설정

**경로**: Repository Settings > Secrets and variables > Actions > Repository secrets

| Secret 이름 | 설명 | 예시 값 |
|------------|------|---------|
| `AZURE_CREDENTIALS` | Azure Service Principal JSON | 위 2.1 참조 |
| `ACR_USERNAME` | ACR 사용자명 | `acrdigitalgarage01` |
| `ACR_PASSWORD` | ACR 패스워드 | `az acr credential show` 결과 |
| `SONAR_HOST_URL` | SonarQube 서버 URL | `http://20.249.187.69` |
| `SONAR_TOKEN` | SonarQube 인증 토큰 | 위 2.2 참조 |
| `DOCKERHUB_USERNAME` | Docker Hub 사용자명 | 본인 Docker Hub ID |
| `DOCKERHUB_PASSWORD` | Docker Hub 패스워드/토큰 | 위 2.3 참조 |

**AZURE_CREDENTIALS 예시:**
```json
{
  "clientId": "5e4b5b41-7208-48b7-b821-d6d5acf50ecf",
  "clientSecret": "ldu8Q~GQEzFYU.dJX7_QsahR7n7C2xqkIM6hqbV8",
  "subscriptionId": "2513dd36-7978-48e3-9a7c-b221d4874f66",
  "tenantId": "4f0a3bfd-1156-4cce-8dc2-a049a13dba23"
}
```

### 3.2 Repository Variables 설정

**경로**: Repository Settings > Secrets and variables > Actions > Variables > Repository variables

| Variable 이름 | 설명 | 기본값 |
|--------------|------|--------|
| `ENVIRONMENT` | 배포 환경 | `dev` |
| `SKIP_SONARQUBE` | SonarQube 분석 건너뛰기 | `true` |

**사용 방법:**
- **자동 실행** (Push/PR): 기본값 사용 (`ENVIRONMENT=dev`, `SKIP_SONARQUBE=true`)
- **수동 실행**: Actions 탭 > "Backend Services CI/CD" > "Run workflow" 버튼
  - Environment: `dev` / `staging` / `prod` 선택
  - Skip SonarQube Analysis: `true` / `false` 선택

---

## 📂 4. 디렉토리 구조

```
.github/
├── kustomize/                    # Kustomize 매니페스트
│   ├── base/                     # Base 매니페스트
│   │   ├── kustomization.yaml
│   │   ├── common/               # 공통 리소스
│   │   │   ├── cm-common.yaml
│   │   │   ├── secret-common.yaml
│   │   │   ├── secret-imagepull.yaml
│   │   │   └── ingress.yaml
│   │   ├── api-gateway/
│   │   │   ├── deployment.yaml
│   │   │   └── service.yaml
│   │   ├── user-service/
│   │   │   ├── deployment.yaml
│   │   │   ├── service.yaml
│   │   │   ├── cm-user-service.yaml
│   │   │   └── secret-user-service.yaml
│   │   ├── bill-service/
│   │   │   ├── deployment.yaml
│   │   │   ├── service.yaml
│   │   │   ├── cm-bill-service.yaml
│   │   │   └── secret-bill-service.yaml
│   │   ├── product-service/
│   │   │   ├── deployment.yaml
│   │   │   ├── service.yaml
│   │   │   ├── cm-product-service.yaml
│   │   │   └── secret-product-service.yaml
│   │   └── kos-mock/
│   │       ├── deployment.yaml
│   │       ├── service.yaml
│   │       └── cm-kos-mock.yaml
│   └── overlays/                 # 환경별 Overlay
│       ├── dev/
│       │   ├── kustomization.yaml
│       │   ├── cm-common-patch.yaml
│       │   ├── secret-common-patch.yaml
│       │   ├── ingress-patch.yaml
│       │   ├── deployment-api-gateway-patch.yaml
│       │   ├── deployment-user-service-patch.yaml
│       │   ├── secret-user-service-patch.yaml
│       │   ├── deployment-bill-service-patch.yaml
│       │   ├── secret-bill-service-patch.yaml
│       │   ├── deployment-product-service-patch.yaml
│       │   ├── secret-product-service-patch.yaml
│       │   ├── deployment-kos-mock-patch.yaml
│       │   └── cm-kos-mock-patch.yaml
│       ├── staging/              # staging도 dev와 동일 구조
│       └── prod/                 # prod도 dev와 동일 구조
├── config/                       # 환경별 설정
│   ├── deploy_env_vars_dev
│   ├── deploy_env_vars_staging
│   └── deploy_env_vars_prod
├── scripts/                      # 배포 스크립트
│   └── deploy-actions.sh
└── workflows/                    # GitHub Actions 워크플로우
    └── backend-cicd.yaml
```

---

## 🚀 5. 파이프라인 구조

### 5.1 워크플로우 단계

```mermaid
graph LR
    A[Push/PR] --> B[Build & Test]
    B --> C{SonarQube<br/>Skip?}
    C -->|No| D[SonarQube Analysis]
    C -->|Yes| E[Skip Analysis]
    D --> F[Quality Gate]
    E --> F
    F --> G[Build Docker Images]
    G --> H[Push to ACR]
    H --> I[Deploy to AKS]
    I --> J[Health Check]
```

### 5.2 Job 구성

1. **build**: 빌드 및 테스트
   - Gradle 빌드
   - JUnit 단위 테스트
   - SonarQube 분석 (선택적)
   - 빌드 아티팩트 업로드

2. **release**: Docker 이미지 빌드 및 푸시
   - 빌드 아티팩트 다운로드
   - Docker 이미지 빌드
   - ACR에 푸시 (환경별 태그)

3. **deploy**: Kubernetes 배포
   - AKS 인증
   - Kustomize로 매니페스트 생성
   - kubectl로 배포
   - Health Check

---

## 📝 6. Kustomize 설계 원칙

### 6.1 Base 매니페스트
- 모든 환경에서 공통으로 사용되는 리소스 정의
- 네임스페이스 하드코딩 **제거** (Overlay에서 지정)
- 이미지 태그는 `latest`로 기본값 설정

### 6.2 Overlay Patch 원칙

**⚠️ 중요 원칙:**
1. **Base에 없는 항목 추가 금지**: Patch는 기존 항목만 수정
2. **Base와 항목 일치 필수**: 구조가 정확히 일치해야 함
3. **Secret은 `stringData` 사용**: `data`(base64) 대신 평문 사용
4. **Patch 방법**: `patches` + `target` 명시 (deprecated `patchesStrategicMerge` 사용 안함)

### 6.3 환경별 차이

| 항목 | dev | staging | prod |
|------|-----|---------|------|
| **Replicas** | 1 | 2 | 3 |
| **CPU Request** | 256m | 512m | 1024m |
| **Memory Request** | 256Mi | 512Mi | 1024Mi |
| **CPU Limit** | 1024m | 2048m | 4096m |
| **Memory Limit** | 1024Mi | 2048Mi | 4096Mi |
| **DDL Auto** | update | validate | validate |
| **JWT Expiration** | 3600000 (1h) | 3600000 (1h) | 1800000 (30m) |
| **Ingress HTTPS** | false | true | true |
| **Profile** | dev | staging | prod |

---

## 🔄 7. 배포 플로우

### 7.1 자동 배포 (Push/PR)

```bash
# main 또는 develop 브랜치에 Push
git add .
git commit -m "feat: 새로운 기능 추가"
git push origin main

# GitHub Actions 자동 실행
# 1. Build & Test (SKIP_SONARQUBE=true)
# 2. Docker Build & Push (dev-{timestamp} 태그)
# 3. Deploy to dev environment
```

### 7.2 수동 배포 (GitHub UI)

1. GitHub Repository > **Actions** 탭
2. **Backend Services CI/CD** 워크플로우 선택
3. **Run workflow** 버튼 클릭
4. 옵션 선택:
   - **Environment**: `dev` / `staging` / `prod`
   - **Skip SonarQube Analysis**: `true` / `false`
5. **Run workflow** 실행

### 7.3 로컬 수동 배포 (CLI)

```bash
# 배포 스크립트 실행
cd /path/to/phonebill
.github/scripts/deploy-actions.sh {환경} {이미지태그}

# 예시: dev 환경에 20250101120000 태그 배포
.github/scripts/deploy-actions.sh dev 20250101120000

# 예시: prod 환경에 latest 배포
.github/scripts/deploy-actions.sh prod latest
```

---

## 🔙 8. 롤백 방법

### 8.1 GitHub Actions로 롤백

1. GitHub Repository > **Actions** 탭
2. **성공한 이전 워크플로우** 선택
3. **Re-run all jobs** 버튼 클릭

### 8.2 kubectl로 롤백

```bash
# 이전 버전으로 즉시 롤백
kubectl rollout undo deployment/dev-user-service -n phonebill-dg0500

# 특정 리비전으로 롤백
kubectl rollout history deployment/dev-user-service -n phonebill-dg0500
kubectl rollout undo deployment/dev-user-service -n phonebill-dg0500 --to-revision=2

# 롤백 상태 확인
kubectl rollout status deployment/dev-user-service -n phonebill-dg0500
```

### 8.3 수동 스크립트로 롤백

```bash
# 안정적인 이전 이미지 태그로 재배포
.github/scripts/deploy-actions.sh dev 20241231235959
```

---

## 🧪 9. SonarQube 설정

### 9.1 Quality Gate 기준

| 지표 | 임계값 |
|------|--------|
| **Coverage** | ≥ 80% |
| **Duplicated Lines** | ≤ 3% |
| **Maintainability Rating** | ≤ A |
| **Reliability Rating** | ≤ A |
| **Security Rating** | ≤ A |

### 9.2 프로젝트 생성

각 서비스별로 환경별 프로젝트 생성:
- `phonebill-api-gateway-dev`
- `phonebill-user-service-dev`
- `phonebill-bill-service-dev`
- `phonebill-product-service-dev`
- `phonebill-kos-mock-dev`
- `phonebill-api-gateway-staging`
- ...
- `phonebill-api-gateway-prod`
- ...

### 9.3 분석 실행

```bash
# 모든 서비스 분석 (로컬 테스트)
./gradlew test jacocoTestReport sonar \
  -Dsonar.projectKey=phonebill-user-service-dev \
  -Dsonar.projectName=phonebill-user-service-dev \
  -Dsonar.host.url=http://20.249.187.69 \
  -Dsonar.token={YOUR_TOKEN}
```

---

## ✅ 10. 체크리스트

### 10.1 사전 준비
- [ ] Azure Service Principal 생성 완료
- [ ] ACR Credentials 확인 완료
- [ ] SonarQube 토큰 생성 완료
- [ ] Docker Hub 토큰 생성 완료
- [ ] GitHub Repository Secrets 등록 완료
- [ ] GitHub Repository Variables 등록 완료

### 10.2 Kustomize Base
- [ ] `.github/kustomize/base/` 디렉토리 생성
- [ ] 기존 `deployment/k8s/` 파일 복사 완료
- [ ] 네임스페이스 하드코딩 제거 완료
- [ ] `base/kustomization.yaml` 생성 완료
- [ ] `kubectl kustomize .github/kustomize/base/` 정상 실행 확인

### 10.3 Kustomize Overlay (dev)
- [ ] `.github/kustomize/overlays/dev/kustomization.yaml` 생성
- [ ] `cm-common-patch.yaml` 생성 (dev 프로파일)
- [ ] `secret-common-patch.yaml` 생성
- [ ] `ingress-patch.yaml` 생성 (base와 동일 host)
- [ ] 각 서비스별 `deployment-{service}-patch.yaml` 생성 (replicas=1, resources)
- [ ] 각 서비스별 `secret-{service}-patch.yaml` 생성 (필요시)
- [ ] `kubectl kustomize .github/kustomize/overlays/dev/` 정상 실행 확인

### 10.4 Kustomize Overlay (staging/prod)
- [ ] staging 환경 모든 파일 생성 (replicas=2, HTTPS)
- [ ] prod 환경 모든 파일 생성 (replicas=3, HTTPS, 짧은 JWT)
- [ ] `kubectl kustomize .github/kustomize/overlays/{env}/` 정상 실행 확인

### 10.5 GitHub Actions
- [ ] `.github/workflows/backend-cicd.yaml` 생성
- [ ] JDK 버전 확인 (`java-version: '21'`)
- [ ] 모든 서비스명 치환 확인
- [ ] SKIP_SONARQUBE 조건부 처리 확인
- [ ] 워크플로우 문법 검증 (GitHub에서 자동 검증)

### 10.6 환경 설정 및 스크립트
- [ ] `.github/config/deploy_env_vars_dev` 생성
- [ ] `.github/config/deploy_env_vars_staging` 생성
- [ ] `.github/config/deploy_env_vars_prod` 생성
- [ ] `.github/scripts/deploy-actions.sh` 생성
- [ ] 스크립트 실행 권한 설정 (`chmod +x`)

### 10.7 배포 테스트
- [ ] GitHub Actions 수동 실행 테스트 (dev)
- [ ] 배포 성공 확인 (`kubectl get pods`)
- [ ] Health Check 확인 (`/actuator/health`)
- [ ] 롤백 테스트 수행
- [ ] SonarQube 분석 테스트 (SKIP=false)

---

## 📚 11. 참고 자료

### 11.1 문서
- [Kustomize 공식 문서](https://kustomize.io/)
- [GitHub Actions 문서](https://docs.github.com/en/actions)
- [SonarQube 문서](https://docs.sonarqube.org/)
- [Azure CLI 문서](https://docs.microsoft.com/en-us/cli/azure/)

### 11.2 주요 명령어

```bash
# Kustomize 빌드 확인
kubectl kustomize .github/kustomize/overlays/dev/

# 배포 상태 확인
kubectl get pods -n phonebill-dg0500
kubectl get deployments -n phonebill-dg0500
kubectl get services -n phonebill-dg0500
kubectl get ingress -n phonebill-dg0500

# 로그 확인
kubectl logs -n phonebill-dg0500 deployment/dev-user-service

# Rollout 히스토리 확인
kubectl rollout history deployment/dev-user-service -n phonebill-dg0500
```

---

## 🆘 12. 트러블슈팅

### 12.1 이미지 Pull 실패
**증상**: `ImagePullBackOff` 에러
**원인**: ACR 인증 실패
**해결**:
```bash
# Secret 확인
kubectl get secret secret-imagepull -n phonebill-dg0500 -o yaml

# Secret 재생성
kubectl create secret docker-registry secret-imagepull \
  --docker-server=acrdigitalgarage01.azurecr.io \
  --docker-username={ACR_USERNAME} \
  --docker-password={ACR_PASSWORD} \
  -n phonebill-dg0500 --dry-run=client -o yaml | kubectl apply -f -
```

### 12.2 Kustomize 빌드 실패
**증상**: `Error: accumulating resources` 에러
**원인**: YAML 문법 오류 또는 파일 경로 오류
**해결**:
```bash
# 문법 검증
kubectl kustomize .github/kustomize/overlays/dev/ --enable-helm

# 파일 존재 여부 확인
ls -la .github/kustomize/base/
ls -la .github/kustomize/overlays/dev/
```

### 12.3 SonarQube Quality Gate 실패
**증상**: Quality Gate 통과 실패
**원인**: Coverage 부족, 코드 품질 이슈
**해결**:
```bash
# 로컬에서 테스트 커버리지 확인
./gradlew :user-service:test :user-service:jacocoTestReport

# 리포트 확인
open user-service/build/reports/jacoco/test/html/index.html
```

---

## 🎯 13. 다음 단계

1. **모니터링 추가**: Prometheus + Grafana 연동
2. **알림 설정**: Slack/Teams 알림 연동
3. **보안 강화**: Trivy 이미지 스캔 추가
4. **성능 테스트**: JMeter/Gatling 연동
5. **Blue/Green 배포**: 무중단 배포 전략 구현

---

**작성일**: 2025-01-01
**작성자**: DevOps Team
**버전**: 1.0.0
