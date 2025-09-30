# phonebill Jenkins CI/CD 파이프라인 구축 가이드

## 📋 개요

본 가이드는 phonebill 프로젝트를 위한 Jenkins + Kustomize 기반 CI/CD 파이프라인 구축 방법을 제공합니다.

### 프로젝트 정보
- **시스템명**: phonebill
- **서비스 목록**: api-gateway, user-service, bill-service, product-service, kos-mock
- **JDK 버전**: 21
- **Azure 환경**:
  - ACR: acrdigitalgarage01
  - 리소스 그룹: rg-digitalgarage-01
  - AKS 클러스터: aks-digitalgarage-01
  - 네임스페이스: phonebill-dg0500

## 🏗️ 파이프라인 아키텍처

### 주요 구성 요소
- **빌드**: Gradle 기반 멀티모듈 빌드
- **품질 검증**: SonarQube 분석 & Quality Gate
- **컨테이너화**: Podman 기반 이미지 빌드
- **배포**: Kustomize를 통한 환경별 배포
- **인프라**: AKS (Azure Kubernetes Service)

### 환경별 설정
| 환경 | Replicas | CPU Request | Memory Request | CPU Limit | Memory Limit |
|------|----------|-------------|----------------|-----------|--------------|
| dev | 1 | 256m | 256Mi | 1024m | 1024Mi |
| staging | 2 | 512m | 512Mi | 2048m | 2048Mi |
| prod | 3 | 1024m | 1024Mi | 4096m | 4096Mi |

## 🚀 Jenkins 서버 환경 구성

### 1. Jenkins 필수 플러그인 설치

Jenkins 관리 > Plugin Manager에서 다음 플러그인들을 설치해주세요:

```bash
# Jenkins 필수 플러그인 목록
- Kubernetes
- Pipeline Utility Steps
- Docker Pipeline
- GitHub
- SonarQube Scanner
- Azure Credentials
```

### 2. Jenkins Credentials 등록

**Manage Jenkins > Credentials > Add Credentials**에서 다음 인증 정보들을 등록해주세요:

#### Azure Service Principal
```
Kind: Microsoft Azure Service Principal
ID: azure-credentials
Subscription ID: {구독ID}
Client ID: {클라이언트ID}
Client Secret: {클라이언트시크릿}
Tenant ID: {테넌트ID}
Azure Environment: Azure
```

#### ACR Credentials
```
Kind: Username with password
ID: acr-credentials
Username: acrdigitalgarage01
Password: {ACR_PASSWORD}
```

#### Docker Hub Credentials (Rate Limit 해결용)
```
Kind: Username with password
ID: dockerhub-credentials
Username: {DOCKERHUB_USERNAME}
Password: {DOCKERHUB_PASSWORD}
참고: Docker Hub 무료 계정 생성 (https://hub.docker.com)
```

#### SonarQube Token
```
Kind: Secret text
ID: sonarqube-token
Secret: {SonarQube토큰}
```

## 📂 Kustomize 구조

```
deployment/cicd/kustomize/
├── base/
│   ├── kustomization.yaml          # Base 설정
│   ├── common/                     # 공통 리소스
│   │   ├── cm-common.yaml
│   │   ├── secret-common.yaml
│   │   ├── secret-imagepull.yaml
│   │   └── ingress.yaml
│   ├── api-gateway/                # API Gateway 리소스
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── cm-api-gateway.yaml
│   ├── user-service/               # User Service 리소스
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   ├── cm-user-service.yaml
│   │   └── secret-user-service.yaml
│   ├── bill-service/               # Bill Service 리소스
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   ├── cm-bill-service.yaml
│   │   └── secret-bill-service.yaml
│   ├── product-service/            # Product Service 리소스
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   ├── cm-product-service.yaml
│   │   └── secret-product-service.yaml
│   └── kos-mock/                   # KOS Mock 리소스
│       ├── deployment.yaml
│       ├── service.yaml
│       └── cm-kos-mock.yaml
└── overlays/
    ├── dev/                        # 개발 환경
    │   ├── kustomization.yaml
    │   ├── cm-common-patch.yaml
    │   ├── secret-common-patch.yaml
    │   ├── ingress-patch.yaml
    │   ├── deployment-*-patch.yaml (각 서비스별)
    │   └── secret-*-patch.yaml (Secret 보유 서비스)
    ├── staging/                    # 스테이징 환경
    │   └── (dev와 동일한 구조)
    └── prod/                       # 운영 환경
        └── (dev와 동일한 구조)
```

## 🔧 Jenkins Pipeline Job 생성

### 1. 새 Pipeline Job 생성
1. Jenkins 대시보드에서 **New Item** 클릭
2. **Pipeline** 선택하고 프로젝트명 입력
3. **OK** 클릭

### 2. Pipeline 설정
#### General 탭
- **Build Parameters** 추가:
  ```
  ENVIRONMENT: Choice Parameter
    Choices: dev, staging, prod
    Default: dev

  IMAGE_TAG: String Parameter
    Default: latest
    Description: 이미지 태그 (기본값: latest)

  SKIP_SONARQUBE: String Parameter
    Default: true
    Description: SonarQube 분석 건너뛰기 (true/false)
  ```

#### Pipeline 탭
```
Definition: Pipeline script from SCM
SCM: Git
Repository URL: {Git저장소URL}
Branch: main (또는 develop)
Script Path: deployment/cicd/Jenkinsfile
```

## 📊 SonarQube 프로젝트 설정

### 1. 각 서비스별 SonarQube 프로젝트 생성
- phonebill-api-gateway-{환경}
- phonebill-user-service-{환경}
- phonebill-bill-service-{환경}
- phonebill-product-service-{환경}
- phonebill-kos-mock-{환경}

### 2. Quality Gate 설정
```
Coverage: >= 80%
Duplicated Lines: <= 3%
Maintainability Rating: <= A
Reliability Rating: <= A
Security Rating: <= A
```

## 🚀 배포 실행 방법

### 1. Jenkins Pipeline 실행
1. Jenkins > {프로젝트명} > **Build with Parameters** 클릭
2. 파라미터 설정:
   - **ENVIRONMENT**: dev/staging/prod 선택
   - **IMAGE_TAG**: 이미지 태그 입력 (선택사항)
   - **SKIP_SONARQUBE**: SonarQube 분석 건너뛰려면 "true", 실행하려면 "false"
3. **Build** 클릭

### 2. 수동 배포 (스크립트 사용)
```bash
# 개발 환경 배포
./deployment/cicd/scripts/deploy.sh dev 20241230120000

# 스테이징 환경 배포
./deployment/cicd/scripts/deploy.sh staging 20241230120000

# 운영 환경 배포
./deployment/cicd/scripts/deploy.sh prod 20241230120000
```

### 3. 배포 상태 확인
```bash
# Pod 상태 확인
kubectl get pods -n phonebill-dg0500

# 서비스 상태 확인
kubectl get services -n phonebill-dg0500

# Ingress 상태 확인
kubectl get ingress -n phonebill-dg0500

# 특정 서비스 로그 확인
kubectl logs -f deployment/user-service -n phonebill-dg0500
```

## 🔍 리소스 검증

### 리소스 검증 스크립트 실행
```bash
# 모든 Kustomize 리소스 검증
./deployment/cicd/scripts/validate-resources.sh
```

### 수동 검증 명령어
```bash
# Base 검증
kubectl kustomize deployment/cicd/kustomize/base/

# 환경별 검증
kubectl kustomize deployment/cicd/kustomize/overlays/dev/
kubectl kustomize deployment/cicd/kustomize/overlays/staging/
kubectl kustomize deployment/cicd/kustomize/overlays/prod/
```

## 🔄 롤백 방법

### 1. Kubernetes 롤백
```bash
# 특정 버전으로 롤백
kubectl rollout undo deployment/{서비스명} -n phonebill-dg0500 --to-revision=2

# 롤백 상태 확인
kubectl rollout status deployment/{서비스명} -n phonebill-dg0500
```

### 2. 이미지 태그 기반 롤백
```bash
# 이전 안정 버전 이미지 태그로 업데이트
cd deployment/cicd/kustomize/overlays/{환경}
kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/{서비스명}:{환경}-{이전태그}
kubectl apply -k .
```

## 🎯 파이프라인 단계별 설명

### 1. Get Source
- Git 저장소에서 소스코드 체크아웃
- 환경별 설정 파일 로드

### 2. Setup AKS
- Azure CLI를 통한 AKS 클러스터 연결
- 네임스페이스 생성 (phonebill-dg0500)

### 3. Build
- Gradle을 통한 멀티모듈 빌드
- 테스트는 SonarQube 단계에서 실행

### 4. SonarQube Analysis & Quality Gate
- 각 서비스별 개별 테스트 실행
- JaCoCo 코드 커버리지 리포트 생성
- SonarQube 정적 분석
- Quality Gate 통과 검증

### 5. Build & Push Images
- Podman을 통한 컨테이너 이미지 빌드
- 환경별 태그로 ACR에 푸시
- 30분 타임아웃 설정

### 6. Update Kustomize & Deploy
- Kustomize를 통한 이미지 태그 업데이트
- 환경별 매니페스트 적용
- 배포 완료까지 대기 (5분 타임아웃)

### 7. Pipeline Complete
- 파이프라인 완료 상태 로깅
- Pod 자동 정리 (3초 후 종료)

## ⚠️ 주의사항

### 1. 파드 자동 정리
- 파이프라인 완료 시 에이전트 파드 자동 삭제
- `podRetention: never()` 설정으로 즉시 정리
- 리소스 절약을 위한 필수 설정

### 2. 변수 참조 문법
- Jenkins Groovy에서는 `${variable}` 사용
- `\${variable}` 사용 시 "syntax error: bad substitution" 오류 발생

### 3. 쉘 호환성
- Jenkins 컨테이너 기본 쉘이 `/bin/sh`인 경우
- Bash 배열 문법 `()` 미지원으로 공백 구분 문자열 사용

### 4. 환경별 설정 차이점
- **DEV**: SSL 리다이렉트 비활성화, 기본 도메인 사용
- **STAGING/PROD**: SSL 리다이렉트 활성화, 사용자 정의 도메인, 인증서 설정

## 🛠️ 트러블슈팅

### 1. 빌드 실패
```bash
# Gradle 캐시 클리어
./gradlew clean build

# JDK 버전 확인
java -version
```

### 2. 이미지 푸시 실패
```bash
# ACR 로그인 확인
az acr login --name acrdigitalgarage01

# Docker Hub rate limit 확인
docker info | grep -i rate
```

### 3. 배포 실패
```bash
# 네임스페이스 확인
kubectl get namespaces

# 리소스 상태 확인
kubectl describe deployment/{서비스명} -n phonebill-dg0500
```

### 4. SonarQube 분석 실패
- SonarQube 서버 연결 상태 확인
- 토큰 유효성 검증
- Quality Gate 설정 확인

## 📈 모니터링 및 로그

### 1. Jenkins 빌드 히스토리
- 각 단계별 실행 시간 추적
- 실패 단계 및 원인 분석

### 2. 애플리케이션 로그
```bash
# 실시간 로그 모니터링
kubectl logs -f deployment/{서비스명} -n phonebill-dg0500

# 특정 기간 로그 확인
kubectl logs deployment/{서비스명} -n phonebill-dg0500 --since=1h
```

### 3. 성능 메트릭
- Kubernetes 메트릭 서버 활용
- 각 서비스별 리소스 사용량 모니터링

---

## 📞 지원

문의사항이나 문제 발생 시:
1. Jenkins 빌드 로그 확인
2. Kubernetes 이벤트 확인: `kubectl get events -n phonebill-dg0500`
3. 리소스 검증 스크립트 실행: `./deployment/cicd/scripts/validate-resources.sh`

**최운영/데옵스**: phonebill Jenkins CI/CD 파이프라인이 성공적으로 구축되었습니다! 🎉