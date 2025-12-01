# Jenkins CI/CD 파이프라인 가이드

## 개요
이 문서는 Phonebill 프로젝트의 Jenkins 기반 CI/CD 파이프라인 구축 및 운영 가이드입니다.

## 프로젝트 정보

| 항목 | 값 |
|------|-----|
| 시스템명 | phonebill |
| JDK 버전 | 21 |
| Image Registry | docker.io |
| Image Organization | hiondal |
| K8s Context | minikube-remote |
| Namespace | phonebill |

### 서비스 목록
- api-gateway (포트: 8080)
- user-service (포트: 8081)
- bill-service (포트: 8082)
- product-service (포트: 8083)
- kos-mock (포트: 8084)

---

## 1. 사전 준비사항

### 1.1 Jenkins 필수 플러그인
```
- Kubernetes
- Pipeline Utility Steps
- Docker Pipeline
- GitHub
- SonarQube Scanner
```

### 1.2 Jenkins Credentials 등록

Jenkins 관리 > Credentials > Add Credentials에서 등록:

#### Docker Hub Credentials
```
- Kind: Username with password
- ID: dockerhub-credentials
- Username: {DOCKERHUB_USERNAME}
- Password: {DOCKERHUB_PASSWORD}
```

#### SonarQube Token (선택사항)
```
- Kind: Secret text
- ID: sonarqube-token
- Secret: {SonarQube 토큰}
```

### 1.3 SonarQube 설정 (선택사항)
Jenkins 관리 > Configure System > SonarQube servers:
```
- Name: SonarQube
- Server URL: http://{SONARQUBE_URL}
- Server authentication token: sonarqube-token (위에서 등록한 credential)
```

---

## 2. Kustomize 구조

### 2.1 디렉토리 구조
```
deployment/cicd/
├── Jenkinsfile
├── config/
│   ├── deploy_env_vars_dev
│   ├── deploy_env_vars_staging
│   └── deploy_env_vars_prod
├── scripts/
│   ├── deploy.sh
│   └── validate-resources.sh
└── kustomize/
    ├── base/
    │   ├── kustomization.yaml
    │   ├── common/
    │   │   ├── cm-common.yaml
    │   │   ├── secret-common.yaml
    │   │   └── ingress.yaml
    │   ├── api-gateway/
    │   ├── user-service/
    │   ├── bill-service/
    │   ├── product-service/
    │   └── kos-mock/
    └── overlays/
        ├── dev/
        ├── staging/
        └── prod/
```

### 2.2 환경별 설정 차이

| 항목 | dev | staging | prod |
|------|-----|---------|------|
| Namespace | phonebill | phonebill-staging | phonebill-prod |
| Replicas | 1 | 2 | 3 |
| CPU Requests | 256m | 512m | 1024m |
| Memory Requests | 256Mi | 512Mi | 1024Mi |
| CPU Limits | 1024m | 2048m | 4096m |
| Memory Limits | 1024Mi | 2048Mi | 4096Mi |
| DDL_AUTO | update | validate | validate |
| SHOW_SQL | true | false | false |
| SSL Redirect | false | true | true |

---

## 3. Jenkins Pipeline Job 생성

### 3.1 New Item > Pipeline 선택

### 3.2 Pipeline 설정
```
SCM: Git
Repository URL: {Git 저장소 URL}
Branch: main (또는 develop)
Script Path: deployment/cicd/Jenkinsfile
```

### 3.3 Pipeline Parameters 설정
| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| ENVIRONMENT | Choice | dev | 배포 환경 (dev/staging/prod) |
| SKIP_SONARQUBE | String | true | SonarQube 분석 건너뛰기 (true/false) |

---

## 4. 파이프라인 스테이지

### 4.1 Get Source
- Git 저장소에서 소스 코드 체크아웃
- 환경별 설정 파일 로드

### 4.2 Setup Kubernetes
- Kubernetes 컨텍스트 설정
- 네임스페이스 생성

### 4.3 Build
- Gradle을 사용한 빌드 (테스트 제외)
- `./gradlew build -x test`

### 4.4 SonarQube Analysis & Quality Gate (선택사항)
- SKIP_SONARQUBE=false일 때만 실행
- 각 서비스별 테스트 및 코드 품질 분석
- Quality Gate 통과 확인

### 4.5 Build & Push Images
- Podman을 사용한 컨테이너 이미지 빌드
- Docker Hub로 이미지 푸시
- 이미지 태그: `{환경}-{타임스탬프}`

### 4.6 Update Kustomize & Deploy
- Kustomize를 사용한 이미지 태그 업데이트
- Kubernetes 매니페스트 적용
- 배포 상태 확인

---

## 5. 배포 실행

### 5.1 Jenkins 파이프라인 실행
1. Jenkins > phonebill > Build with Parameters
2. ENVIRONMENT 선택 (dev/staging/prod)
3. SKIP_SONARQUBE 입력 (true 또는 false)
4. Build 클릭

### 5.2 수동 배포 (스크립트 사용)
```bash
# dev 환경 배포
./deployment/cicd/scripts/deploy.sh dev latest

# staging 환경 배포
./deployment/cicd/scripts/deploy.sh staging v1.0.0

# prod 환경 배포
./deployment/cicd/scripts/deploy.sh prod v1.0.0
```

### 5.3 배포 상태 확인
```bash
# Pod 상태 확인
kubectl get pods -n phonebill

# 서비스 상태 확인
kubectl get services -n phonebill

# Ingress 상태 확인
kubectl get ingress -n phonebill

# 특정 Pod 로그 확인
kubectl logs -f deployment/api-gateway -n phonebill
```

---

## 6. 롤백

### 6.1 이전 버전으로 롤백
```bash
# 특정 리비전으로 롤백
kubectl rollout undo deployment/{서비스명} -n phonebill --to-revision=2

# 롤백 상태 확인
kubectl rollout status deployment/{서비스명} -n phonebill

# 롤백 히스토리 확인
kubectl rollout history deployment/{서비스명} -n phonebill
```

### 6.2 이미지 태그 기반 롤백
```bash
cd deployment/cicd/kustomize/overlays/{환경}

# 이전 안정 버전 이미지 태그로 업데이트
kustomize edit set image docker.io/hiondal/{서비스명}:{환경}-{이전태그}

# 배포
kubectl apply -k .
```

---

## 7. 리소스 검증

### 7.1 검증 스크립트 실행
```bash
./deployment/cicd/scripts/validate-resources.sh
```

### 7.2 Kustomize 빌드 테스트
```bash
# Base 빌드 테스트
kubectl kustomize deployment/cicd/kustomize/base/

# 환경별 빌드 테스트
kubectl kustomize deployment/cicd/kustomize/overlays/dev/
kubectl kustomize deployment/cicd/kustomize/overlays/staging/
kubectl kustomize deployment/cicd/kustomize/overlays/prod/
```

---

## 8. SonarQube 설정 (선택사항)

### 8.1 Quality Gate 권장 설정
```
Coverage: >= 80%
Duplicated Lines: <= 3%
Maintainability Rating: <= A
Reliability Rating: <= A
Security Rating: <= A
```

### 8.2 SonarQube 프로젝트 생성
각 서비스별로 다음 형식의 프로젝트 키로 생성:
- `phonebill-user-service-dev`
- `phonebill-user-service-staging`
- `phonebill-user-service-prod`

---

## 9. 트러블슈팅

### 9.1 이미지 푸시 실패
- Docker Hub 인증 정보 확인
- Rate Limit 확인 (무료 계정 제한)

### 9.2 배포 실패
```bash
# Pod 이벤트 확인
kubectl describe pod {POD_NAME} -n phonebill

# Pod 로그 확인
kubectl logs {POD_NAME} -n phonebill
```

### 9.3 Kustomize 빌드 실패
- 리소스 파일 존재 여부 확인
- YAML 문법 검증
- `kubectl kustomize` 명령으로 디버깅

### 9.4 SonarQube 연결 실패
- SonarQube 서버 URL 확인
- 인증 토큰 유효성 확인
- 네트워크 연결 확인

---

## 10. 체크리스트

### 사전 준비
- [ ] Jenkins 필수 플러그인 설치 완료
- [ ] Docker Hub Credentials 등록 완료
- [ ] SonarQube Token 등록 완료 (선택)
- [ ] Kubernetes 컨텍스트 설정 완료

### 배포 전
- [ ] 리소스 검증 스크립트 실행 완료
- [ ] 환경별 설정 파일 확인 완료
- [ ] Kustomize 빌드 테스트 완료

### 배포 후
- [ ] Pod 상태 확인 (Running)
- [ ] 서비스 엔드포인트 확인
- [ ] Ingress 접근 테스트
- [ ] 로그 이상 여부 확인
