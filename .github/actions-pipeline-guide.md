# GitHub Actions CI/CD 파이프라인 구축 가이드


## 개요

phonebill 시스템을 위한 GitHub Actions 기반 CI/CD 파이프라인이 성공적으로 구축되었습니다.

### 프로젝트 정보
- **시스템명**: phonebill
- **서비스**: api-gateway, user-service, bill-service, product-service, kos-mock
- **JDK 버전**: 21
- **Azure 환경**: ACR(acrdigitalgarage01), AKS(aks-digitalgarage-01), RG(rg-digitalgarage-01)
- **네임스페이스**: phonebill-dg0500

## 구축된 파일 구조

```
.github/
├── kustomize/
│   ├── base/
│   │   ├── kustomization.yaml
│   │   ├── common/
│   │   │   ├── cm-common.yaml
│   │   │   ├── secret-common.yaml
│   │   │   ├── secret-imagepull.yaml
│   │   │   └── ingress.yaml
│   │   └── {서비스명}/
│   │       ├── deployment.yaml
│   │       ├── service.yaml
│   │       ├── cm-{서비스명}.yaml
│   │       └── secret-{서비스명}.yaml (해당되는 경우)
│   └── overlays/
│       ├── dev/
│       │   ├── kustomization.yaml
│       │   ├── cm-common-patch.yaml
│       │   ├── ingress-patch.yaml
│       │   ├── deployment-{서비스명}-patch.yaml
│       │   ├── secret-common-patch.yaml
│       │   └── secret-{서비스명}-patch.yaml
│       ├── staging/
│       │   └── (dev와 동일한 구조, staging 환경 설정)
│       └── prod/
│           └── (dev와 동일한 구조, prod 환경 설정)
├── config/
│   ├── deploy_env_vars_dev
│   ├── deploy_env_vars_staging
│   └── deploy_env_vars_prod
├── scripts/
│   └── deploy-actions.sh
└── workflows/
    └── backend-cicd.yaml
```

## GitHub Repository 설정

### 1. Repository Secrets 설정

GitHub Repository → Settings → Secrets and variables → Actions → Repository secrets에 다음 설정:

```bash
# Azure Service Principal
AZURE_CREDENTIALS
{
  "clientId": "{클라이언트ID}",
  "clientSecret": "{클라이언트시크릿}",
  "subscriptionId": "{구독ID}",
  "tenantId": "{테넌트ID}"
}

# ACR Credentials (az acr credential show --name acrdigitalgarage01)
ACR_USERNAME: acrdigitalgarage01
ACR_PASSWORD: {ACR패스워드}

# SonarQube 설정
SONAR_HOST_URL: http://{External IP}  # k get svc -n sonarqube로 확인
SONAR_TOKEN: {SonarQube토큰}  # SonarQube > My Account > Security에서 생성

# Docker Hub (Rate Limit 방지)
DOCKERHUB_USERNAME: {Docker Hub 사용자명}
DOCKERHUB_PASSWORD: {Docker Hub 패스워드}
```

### 2. Repository Variables 설정

GitHub Repository → Settings → Secrets and variables → Actions → Variables → Repository variables에 다음 설정:

```bash
ENVIRONMENT: dev (기본값)
SKIP_SONARQUBE: true (기본값)
```

## 파이프라인 실행 방법

### 1. 자동 실행
- **트리거**: main/develop 브랜치에 push 또는 main 브랜치에 PR
- **환경**: dev (기본값)
- **SonarQube**: 스킵 (기본값)

### 2. 수동 실행
1. GitHub → Actions 탭 이동
2. "Backend Services CI/CD" 워크플로우 선택
3. "Run workflow" 버튼 클릭
4. 환경 선택 (dev/staging/prod)
5. SonarQube 분석 여부 선택 (true/false)

## 파이프라인 단계

### 1. Build and Test
- Gradle 빌드 (테스트 제외)
- SonarQube 코드 품질 분석 (선택사항)
- 빌드 아티팩트 업로드

### 2. Build and Push Docker Images
- 각 서비스별 Docker 이미지 빌드
- Azure Container Registry에 푸시
- 이미지 태그: `{환경}-{타임스탬프}`

### 3. Deploy to Kubernetes
- Kustomize를 사용한 환경별 매니페스트 생성
- AKS 클러스터에 배포
- 배포 상태 확인

## 환경별 설정

### DEV 환경
- **Replicas**: 1
- **Resources**: requests(256Mi/256m), limits(1024Mi/1024m)
- **DDL**: update
- **SSL**: 비활성화
- **Host**: phonebill-dg0500-api.20.214.196.128.nip.io

### STAGING 환경
- **Replicas**: 2
- **Resources**: requests(512Mi/512m), limits(2048Mi/2048m)
- **DDL**: validate
- **SSL**: 활성화
- **Host**: phonebill-staging.digitalgarage.com
- **JWT**: 운영 환경 토큰 유효시간

### PROD 환경
- **Replicas**: 3
- **Resources**: requests(1024Mi/1024m), limits(4096Mi/4096m)
- **DDL**: validate
- **SSL**: 활성화
- **Host**: phonebill.digitalgarage.com
- **JWT**: 보안 강화된 짧은 토큰 유효시간

## 수동 배포 방법

로컬에서 수동 배포를 수행하려면:

```bash
# 기본 dev 환경으로 배포
./.github/scripts/deploy-actions.sh

# 특정 환경과 이미지 태그로 배포
./.github/scripts/deploy-actions.sh staging 20241001123000

# 권한 오류 시
chmod +x .github/scripts/deploy-actions.sh
```

## 롤백 방법

### 1. GitHub Actions를 통한 롤백
1. GitHub → Actions → 성공한 이전 워크플로우 선택
2. "Re-run all jobs" 클릭

### 2. kubectl을 이용한 롤백
```bash
# 이전 버전으로 롤백
kubectl rollout undo deployment/{서비스명} -n phonebill-dg0500 --to-revision=2

# 롤백 상태 확인
kubectl rollout status deployment/{서비스명} -n phonebill-dg0500
```

### 3. 수동 스크립트를 이용한 롤백
```bash
# 이전 안정 버전 이미지 태그로 배포
./.github/scripts/deploy-actions.sh {환경} {이전태그}
```

## SonarQube Quality Gate 설정

각 서비스별 프로젝트 생성 후 다음 Quality Gate 설정:

```
Coverage: >= 80%
Duplicated Lines: <= 3%
Maintainability Rating: <= A
Reliability Rating: <= A
Security Rating: <= A
```

## 모니터링 및 확인

### 배포 상태 확인
```bash
# Pod 상태 확인
kubectl get pods -n phonebill-dg0500

# 서비스 상태 확인
kubectl get services -n phonebill-dg0500

# Ingress 상태 확인
kubectl get ingress -n phonebill-dg0500

# 로그 확인
kubectl logs -f deployment/{서비스명} -n phonebill-dg0500
```

### 헬스 체크
```bash
# API Gateway 헬스 체크
curl -f http://phonebill-dg0500-api.20.214.196.128.nip.io/actuator/health
```

## 주요 특징

1. **환경별 분리**: dev, staging, prod 환경별 독립적인 설정
2. **Kustomize 사용**: 환경별 매니페스트 관리 자동화
3. **SonarQube 통합**: 코드 품질 분석 및 Quality Gate
4. **Docker 최적화**: Multi-stage 빌드 및 Rate Limit 방지
5. **자동 배포**: Push/PR 시 자동 빌드 및 배포
6. **수동 배포**: 운영진이 필요 시 수동 실행 가능
7. **롤백 지원**: 다양한 방법의 롤백 기능

## 문제 해결

### 일반적인 오류

1. **Azure 인증 실패**
   - AZURE_CREDENTIALS 설정 확인
   - Service Principal 권한 확인

2. **ACR 접근 실패**
   - ACR_USERNAME, ACR_PASSWORD 확인
   - ACR 권한 설정 확인

3. **SonarQube 분석 실패**
   - SONAR_TOKEN, SONAR_HOST_URL 확인
   - SonarQube 서버 접근성 확인

4. **Kustomize 오류**
   - patch 파일 경로 및 target 확인
   - YAML 문법 오류 확인

### 연락처
문제 발생 시 DevOps 팀에 문의하거나 GitHub Issues를 통해 보고해 주세요.

---

**최운영/데옵스**: GitHub Actions CI/CD 파이프라인 구축이 완료되었습니다! 🎉