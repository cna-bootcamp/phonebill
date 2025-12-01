# 백엔드 Jenkins CI/CD 파이프라인 가이드

## 📋 개요

이 가이드는 phonebill 프로젝트의 Jenkins + Kustomize 기반 CI/CD 파이프라인 구축 및 운영 방법을 설명합니다.

## 🔧 사전 준비사항

### 프로젝트 정보
| 항목 | 값 |
|------|-----|
| 시스템명 | phonebill |
| 서비스 목록 | api-gateway, user-service, bill-service, product-service, kos-mock |
| JDK 버전 | 21 |
| Image Registry | docker.io |
| Image Organization | hiondal |
| Jenkins K8s Cloud Name | k8s |
| K8s Context Prefix | minikube |
| Namespace | phonebill |

### Jenkins 서버 환경 구성

#### 1. 필수 플러그인 설치
```
- Kubernetes
- Pipeline Utility Steps
- Docker Pipeline
- GitHub
- SonarQube Scanner
- Azure Credentials
```

#### 2. Jenkins Credentials 등록

**Docker Hub Credentials (Rate Limit 해결용)**
```
- Kind: Username with password
- ID: dockerhub-credentials
- Username: {DOCKERHUB_USERNAME}
- Password: {DOCKERHUB_PASSWORD}
- 참고: Docker Hub 무료 계정 생성 (https://hub.docker.com)
```

**Image Registry Credentials**
```
- Kind: Username with password
- ID: imagereg-credentials
- Username: {REGISTRY_USERNAME}
- Password: {REGISTRY_PASSWORD}
```

**SonarQube Token (선택사항)**
```
- Kind: Secret text
- ID: sonarqube-token
- Secret: {SonarQube토큰}
```

## 📁 디렉토리 구조

```
deployment/cicd/
├── Jenkinsfile                    # Jenkins 파이프라인 정의
├── jenkins-pipeline-guide.md      # 이 가이드 문서
├── config/
│   ├── deploy_env_vars_dev        # dev 환경 설정
│   ├── deploy_env_vars_staging    # staging 환경 설정
│   └── deploy_env_vars_prod       # prod 환경 설정
├── scripts/
│   ├── deploy.sh                  # 수동 배포 스크립트
│   └── validate-resources.sh      # 리소스 검증 스크립트
└── kustomize/
    ├── base/                      # 기본 Kubernetes 매니페스트
    │   ├── kustomization.yaml
    │   ├── common/
    │   │   ├── cm-common.yaml
    │   │   ├── secret-common.yaml
    │   │   └── ingress.yaml
    │   ├── api-gateway/
    │   │   ├── deployment.yaml
    │   │   ├── service.yaml
    │   │   └── cm-api-gateway.yaml
    │   ├── user-service/
    │   │   ├── deployment.yaml
    │   │   ├── service.yaml
    │   │   ├── cm-user-service.yaml
    │   │   └── secret-user-service.yaml
    │   ├── bill-service/
    │   │   ├── deployment.yaml
    │   │   ├── service.yaml
    │   │   ├── cm-bill-service.yaml
    │   │   └── secret-bill-service.yaml
    │   ├── product-service/
    │   │   ├── deployment.yaml
    │   │   ├── service.yaml
    │   │   ├── cm-product-service.yaml
    │   │   └── secret-product-service.yaml
    │   └── kos-mock/
    │       ├── deployment.yaml
    │       ├── service.yaml
    │       └── cm-kos-mock.yaml
    └── overlays/
        ├── dev/                   # 개발 환경 오버레이
        ├── staging/               # 스테이징 환경 오버레이
        └── prod/                  # 운영 환경 오버레이
```

## 🚀 Jenkins Pipeline Job 생성

### 1. Pipeline Job 생성
1. Jenkins 웹 UI에서 **New Item** > **Pipeline** 선택
2. Pipeline script from SCM 설정:
   ```
   SCM: Git
   Repository URL: {Git저장소URL}
   Branch: main (또는 develop)
   Script Path: deployment/cicd/Jenkinsfile
   ```

### 2. Pipeline Parameters 설정
| 파라미터 | 타입 | 값 | 설명 |
|---------|------|-----|------|
| ENVIRONMENT | Choice | dev, staging, prod | 배포 대상 환경 |
| IMAGE_TAG | String | latest | 이미지 태그 (선택) |
| SKIP_SONARQUBE | String | true | SonarQube 분석 건너뛰기 |

## ⚙️ 환경별 설정

### DEV 환경
```yaml
# Replicas: 1
# Resources:
#   requests: 256m CPU, 256Mi Memory
#   limits: 1024m CPU, 1024Mi Memory
# DDL_AUTO: update
# HTTPS: 비활성화
```

### STAGING 환경
```yaml
# Replicas: 2
# Resources:
#   requests: 512m CPU, 512Mi Memory
#   limits: 2048m CPU, 2048Mi Memory
# DDL_AUTO: validate
# HTTPS: 활성화 (SSL 인증서 필요)
```

### PROD 환경
```yaml
# Replicas: 3
# Resources:
#   requests: 1024m CPU, 1024Mi Memory
#   limits: 4096m CPU, 4096Mi Memory
# DDL_AUTO: validate
# HTTPS: 활성화 (SSL 인증서 필요)
```

## 📊 SonarQube 설정

### Quality Gate 설정
```
Coverage: >= 80%
Duplicated Lines: <= 3%
Maintainability Rating: <= A
Reliability Rating: <= A
Security Rating: <= A
```

### 분석 제외 대상
```
**/config/**
**/entity/**
**/dto/**
**/*Application.class
**/exception/**
```

## 🔨 수동 배포

### 배포 스크립트 사용
```bash
# dev 환경 배포
./deployment/cicd/scripts/deploy.sh dev latest

# staging 환경 배포
./deployment/cicd/scripts/deploy.sh staging v1.0.0

# prod 환경 배포
./deployment/cicd/scripts/deploy.sh prod v1.0.0
```

### 리소스 검증
```bash
# Kustomize 리소스 검증
./deployment/cicd/scripts/validate-resources.sh
```

## 🔄 배포 상태 확인

```bash
# 파드 상태 확인
kubectl get pods -n phonebill

# 서비스 상태 확인
kubectl get services -n phonebill

# 인그레스 확인
kubectl get ingress -n phonebill

# 배포 상태 상세 확인
kubectl describe deployment api-gateway -n phonebill
```

## ⏪ 롤백 방법

### 이전 버전으로 롤백
```bash
# 특정 리비전으로 롤백
kubectl rollout undo deployment/{서비스명} -n phonebill --to-revision=2

# 롤백 상태 확인
kubectl rollout status deployment/{서비스명} -n phonebill
```

### 이미지 태그 기반 롤백
```bash
# 이전 안정 버전 이미지 태그로 업데이트
cd deployment/cicd/kustomize/overlays/{환경}
kustomize edit set image docker.io/hiondal/{서비스명}=docker.io/hiondal/{서비스명}:{환경}-{이전태그}
kubectl apply -k .
```

## 🔍 문제 해결

### 일반적인 문제

#### 1. 이미지 풀 실패
```bash
# ImagePullBackOff 상태 확인
kubectl describe pod {pod-name} -n phonebill

# Docker Hub 인증 확인
kubectl get secret -n phonebill
```

#### 2. 파드 시작 실패
```bash
# 로그 확인
kubectl logs {pod-name} -n phonebill

# 이벤트 확인
kubectl get events -n phonebill --sort-by='.lastTimestamp'
```

#### 3. Kustomize 빌드 실패
```bash
# 검증 스크립트 실행
./deployment/cicd/scripts/validate-resources.sh

# 직접 빌드 테스트
kubectl kustomize deployment/cicd/kustomize/overlays/dev
```

### Jenkins 파이프라인 문제

#### 1. Pod 정리가 안 되는 경우
- `podRetention: never()` 설정 확인
- `terminationGracePeriodSeconds: 3` 설정 확인
- Jenkins Kubernetes Plugin 버전 확인

#### 2. 변수 치환 오류
- `${variable}` 사용 (Groovy 문자열 보간)
- `\${variable}` 사용 금지 (bash 이스케이프)

## 📝 체크리스트

### 배포 전 확인
- [ ] 환경 변수 파일 설정 완료
- [ ] Kubernetes 컨텍스트 설정 완료
- [ ] 이미지 레지스트리 인증 설정
- [ ] 네임스페이스 생성 완료
- [ ] 백킹 서비스(DB, Redis) 준비 완료

### 배포 후 확인
- [ ] 모든 파드 Running 상태 확인
- [ ] 서비스 엔드포인트 정상 응답
- [ ] 로그에 에러 없음
- [ ] 헬스체크 정상 통과
