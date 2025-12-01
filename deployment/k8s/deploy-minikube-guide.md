# Minikube 백엔드 서비스 배포 가이드

## 개요
본 문서는 phonebill 프로젝트의 백엔드 마이크로서비스들을 Minikube 환경에 배포하는 방법을 설명합니다.

## 배포 대상 서비스

| 서비스명 | 포트 | 설명 |
|---------|------|------|
| api-gateway | 8080 | API Gateway 서비스 |
| user-service | 8081 | 사용자 인증/관리 서비스 |
| bill-service | 8082 | 요금 조회 서비스 |
| product-service | 8083 | 상품 변경 서비스 |
| kos-mock | 8084 | KOS 목업 서비스 |

## 배포 환경 정보

| 항목 | 값 |
|------|-----|
| Image Registry | docker.io |
| Image Organization | hiondal |
| Kubernetes Context | minikube-remote |
| Namespace | phonebill |
| Replicas | 1 |
| CPU Request/Limit | 256m/1024m |
| Memory Request/Limit | 256Mi/1024Mi |

---

## 사전 준비

### 1. Minikube 상태 확인
```bash
# context 전환
kubectl config use-context minikube-remote

# minikube 상태 확인
minikube status

# 노드 확인
kubectl get nodes
```

### 2. Namespace 생성
```bash
kubectl create namespace phonebill
kubectl config set-context --current --namespace=phonebill
```

### 3. Ingress Controller 활성화
```bash
minikube addons enable ingress
```

### 4. 백킹 서비스 확인
배포 전 PostgreSQL과 Redis가 실행 중인지 확인합니다:
```bash
# PostgreSQL 파드 확인
kubectl get pods -n phonebill | grep postgres

# Redis 파드 확인
kubectl get pods -n phonebill | grep redis
```

---

## Step 1: 컨테이너 이미지 빌드 및 푸시

### 1.1 Gradle 빌드
```bash
cd /Users/dreamondal/home/workspace/phonebill
./gradlew clean build -x test
```

### 1.2 Docker 이미지 빌드
```bash
DOCKER_FILE=deployment/container/Dockerfile-backend
REGISTRY=docker.io
PROJECT=hiondal
TAG=latest

# 각 서비스 이미지 빌드
for service in api-gateway user-service bill-service product-service kos-mock; do
  docker build \
    --platform linux/amd64 \
    --build-arg BUILD_LIB_DIR="${service}/build/libs" \
    --build-arg ARTIFACTORY_FILE="${service}.jar" \
    -f ${DOCKER_FILE} \
    -t ${REGISTRY}/${PROJECT}/${service}:${TAG} .
done
```

### 1.3 Docker Hub 푸시
```bash
# Docker Hub 로그인
docker login

# 이미지 푸시
for service in api-gateway user-service bill-service product-service kos-mock; do
  docker push ${REGISTRY}/${PROJECT}/${service}:${TAG}
done
```

---

## Step 2: Secret 생성

### 2.1 공통 Secret (JWT, Redis)
```bash
kubectl create secret generic phonebill-common-secret \
  --namespace=phonebill \
  --from-literal=JWT_SECRET='your-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256' \
  --from-literal=REDIS_PASSWORD=''
```

### 2.2 서비스별 DB Secret

```bash
# user-service DB Secret
kubectl create secret generic user-service-db-secret \
  --namespace=phonebill \
  --from-literal=DB_USERNAME='postgres' \
  --from-literal=DB_PASSWORD='your-auth-db-password'

# bill-service DB Secret
kubectl create secret generic bill-service-db-secret \
  --namespace=phonebill \
  --from-literal=DB_USERNAME='postgres' \
  --from-literal=DB_PASSWORD='your-bill-db-password'

# product-service DB Secret
kubectl create secret generic product-service-db-secret \
  --namespace=phonebill \
  --from-literal=DB_USERNAME='postgres' \
  --from-literal=DB_PASSWORD='your-product-db-password'
```

---

## Step 3: ConfigMap 생성

### 3.1 공통 ConfigMap
```bash
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: phonebill-common-config
  namespace: phonebill
data:
  SPRING_PROFILES_ACTIVE: "prod"
  REDIS_HOST: "redis-cache.phonebill.svc.cluster.local"
  REDIS_PORT: "6379"
  CORS_ALLOWED_ORIGINS: "*"
  SHOW_SQL: "false"
  DDL_AUTO: "none"
EOF
```

### 3.2 서비스별 ConfigMap

```bash
# API Gateway ConfigMap
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: api-gateway-config
  namespace: phonebill
data:
  SERVER_PORT: "8080"
  USER_SERVICE_URL: "http://user-service:8081"
  BILL_SERVICE_URL: "http://bill-service:8082"
  PRODUCT_SERVICE_URL: "http://product-service:8083"
  KOS_MOCK_URL: "http://kos-mock:8084"
EOF

# User Service ConfigMap
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: user-service-config
  namespace: phonebill
data:
  SERVER_PORT: "8081"
  DB_HOST: "auth-postgres.phonebill.svc.cluster.local"
  DB_PORT: "5432"
  DB_NAME: "phonebill_auth"
  REDIS_DATABASE: "0"
EOF

# Bill Service ConfigMap
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: bill-service-config
  namespace: phonebill
data:
  SERVER_PORT: "8082"
  DB_HOST: "bill-inquiry-postgres.phonebill.svc.cluster.local"
  DB_PORT: "5432"
  DB_NAME: "bill_inquiry"
  REDIS_DATABASE: "1"
  KOS_BASE_URL: "http://kos-mock:8084"
EOF

# Product Service ConfigMap
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: product-service-config
  namespace: phonebill
data:
  SERVER_PORT: "8083"
  DB_HOST: "product-change-postgres.phonebill.svc.cluster.local"
  DB_PORT: "5432"
  DB_NAME: "product_change"
  REDIS_DATABASE: "2"
  KOS_BASE_URL: "http://kos-mock:8084"
EOF

# KOS Mock ConfigMap
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: kos-mock-config
  namespace: phonebill
data:
  SERVER_PORT: "8084"
EOF
```

---

## Step 4: Deployment 생성

### 4.1 API Gateway Deployment
```bash
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
  namespace: phonebill
  labels:
    app: api-gateway
spec:
  replicas: 1
  selector:
    matchLabels:
      app: api-gateway
  template:
    metadata:
      labels:
        app: api-gateway
    spec:
      containers:
      - name: api-gateway
        image: docker.io/hiondal/api-gateway:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: phonebill-common-config
        - configMapRef:
            name: api-gateway-config
        - secretRef:
            name: phonebill-common-secret
        resources:
          requests:
            cpu: "256m"
            memory: "256Mi"
          limits:
            cpu: "1024m"
            memory: "1024Mi"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
EOF
```

### 4.2 User Service Deployment
```bash
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: phonebill
  labels:
    app: user-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
      - name: user-service
        image: docker.io/hiondal/user-service:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 8081
        envFrom:
        - configMapRef:
            name: phonebill-common-config
        - configMapRef:
            name: user-service-config
        - secretRef:
            name: phonebill-common-secret
        - secretRef:
            name: user-service-db-secret
        resources:
          requests:
            cpu: "256m"
            memory: "256Mi"
          limits:
            cpu: "1024m"
            memory: "1024Mi"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
EOF
```

### 4.3 Bill Service Deployment
```bash
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bill-service
  namespace: phonebill
  labels:
    app: bill-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: bill-service
  template:
    metadata:
      labels:
        app: bill-service
    spec:
      containers:
      - name: bill-service
        image: docker.io/hiondal/bill-service:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 8082
        envFrom:
        - configMapRef:
            name: phonebill-common-config
        - configMapRef:
            name: bill-service-config
        - secretRef:
            name: phonebill-common-secret
        - secretRef:
            name: bill-service-db-secret
        resources:
          requests:
            cpu: "256m"
            memory: "256Mi"
          limits:
            cpu: "1024m"
            memory: "1024Mi"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8082
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8082
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
EOF
```

### 4.4 Product Service Deployment
```bash
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: product-service
  namespace: phonebill
  labels:
    app: product-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: product-service
  template:
    metadata:
      labels:
        app: product-service
    spec:
      containers:
      - name: product-service
        image: docker.io/hiondal/product-service:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 8083
        envFrom:
        - configMapRef:
            name: phonebill-common-config
        - configMapRef:
            name: product-service-config
        - secretRef:
            name: phonebill-common-secret
        - secretRef:
            name: product-service-db-secret
        resources:
          requests:
            cpu: "256m"
            memory: "256Mi"
          limits:
            cpu: "1024m"
            memory: "1024Mi"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8083
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8083
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
EOF
```

### 4.5 KOS Mock Deployment
```bash
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kos-mock
  namespace: phonebill
  labels:
    app: kos-mock
spec:
  replicas: 1
  selector:
    matchLabels:
      app: kos-mock
  template:
    metadata:
      labels:
        app: kos-mock
    spec:
      containers:
      - name: kos-mock
        image: docker.io/hiondal/kos-mock:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 8084
        envFrom:
        - configMapRef:
            name: phonebill-common-config
        - configMapRef:
            name: kos-mock-config
        - secretRef:
            name: phonebill-common-secret
        resources:
          requests:
            cpu: "256m"
            memory: "256Mi"
          limits:
            cpu: "1024m"
            memory: "1024Mi"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8084
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8084
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
EOF
```

---

## Step 5: Service 생성

```bash
# API Gateway Service
kubectl apply -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
  namespace: phonebill
spec:
  type: ClusterIP
  selector:
    app: api-gateway
  ports:
  - port: 8080
    targetPort: 8080
    protocol: TCP
EOF

# User Service
kubectl apply -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: user-service
  namespace: phonebill
spec:
  type: ClusterIP
  selector:
    app: user-service
  ports:
  - port: 8081
    targetPort: 8081
    protocol: TCP
EOF

# Bill Service
kubectl apply -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: bill-service
  namespace: phonebill
spec:
  type: ClusterIP
  selector:
    app: bill-service
  ports:
  - port: 8082
    targetPort: 8082
    protocol: TCP
EOF

# Product Service
kubectl apply -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: product-service
  namespace: phonebill
spec:
  type: ClusterIP
  selector:
    app: product-service
  ports:
  - port: 8083
    targetPort: 8083
    protocol: TCP
EOF

# KOS Mock Service
kubectl apply -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: kos-mock
  namespace: phonebill
spec:
  type: ClusterIP
  selector:
    app: kos-mock
  ports:
  - port: 8084
    targetPort: 8084
    protocol: TCP
EOF
```

---

## Step 6: Ingress 생성

```bash
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: phonebill-ingress
  namespace: phonebill
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /\$1
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "60"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "60"
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "60"
spec:
  ingressClassName: nginx
  rules:
  - host: phonebill-api.72.155.72.236.nip.io
    http:
      paths:
      - path: /(.*)
        pathType: ImplementationSpecific
        backend:
          service:
            name: api-gateway
            port:
              number: 8080
EOF
```

> **참고**: nip.io 도메인을 사용하므로 별도의 hosts 파일 설정이 필요하지 않습니다.

---

## Step 7: 배포 확인

### 7.1 파드 상태 확인
```bash
kubectl get pods -n phonebill -w
```

### 7.2 로그 확인
```bash
# 각 서비스 로그 확인
kubectl logs -f deployment/api-gateway -n phonebill
kubectl logs -f deployment/user-service -n phonebill
kubectl logs -f deployment/bill-service -n phonebill
kubectl logs -f deployment/product-service -n phonebill
kubectl logs -f deployment/kos-mock -n phonebill
```

### 7.3 서비스 엔드포인트 확인
```bash
kubectl get endpoints -n phonebill
```

### 7.4 Ingress 상태 확인
```bash
kubectl get ingress -n phonebill
```

---

## Step 8: API 테스트

### 8.1 Health Check
```bash
# API Gateway Health Check
curl http://phonebill-api.72.155.72.236.nip.io/actuator/health
```

### 8.2 서비스 API 테스트
```bash
# 사용자 등록 API
curl -X POST http://phonebill-api.72.155.72.236.nip.io/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "testuser",
    "password": "Test1234!",
    "name": "테스트사용자",
    "email": "test@example.com",
    "phoneNumber": "01012345678"
  }'

# 로그인 API
curl -X POST http://phonebill-api.72.155.72.236.nip.io/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "testuser",
    "password": "Test1234!"
  }'
```

---

## 트러블슈팅

### 파드가 시작되지 않는 경우
```bash
# 파드 상세 정보 확인
kubectl describe pod <pod-name> -n phonebill

# 이벤트 확인
kubectl get events -n phonebill --sort-by='.lastTimestamp'
```

### 이미지 풀 실패
```bash
# Docker Hub 인증 Secret 생성 (필요시)
kubectl create secret docker-registry regcred \
  --namespace=phonebill \
  --docker-server=https://index.docker.io/v1/ \
  --docker-username=<username> \
  --docker-password=<password> \
  --docker-email=<email>

# Deployment에 imagePullSecrets 추가
kubectl patch deployment <deployment-name> -n phonebill \
  -p '{"spec":{"template":{"spec":{"imagePullSecrets":[{"name":"regcred"}]}}}}'
```

### DB 연결 실패
```bash
# PostgreSQL 서비스 확인
kubectl get svc -n phonebill | grep postgres

# DNS 해석 테스트
kubectl run -it --rm debug --image=busybox --restart=Never -n phonebill \
  -- nslookup auth-postgres.phonebill.svc.cluster.local
```

### Redis 연결 실패
```bash
# Redis 서비스 확인
kubectl get svc -n phonebill | grep redis

# Redis 연결 테스트
kubectl run -it --rm redis-cli --image=redis:alpine --restart=Never -n phonebill \
  -- redis-cli -h redis-cache.phonebill.svc.cluster.local ping
```

---

## 리소스 정리

### 전체 삭제
```bash
kubectl delete namespace phonebill
```

### 개별 삭제
```bash
# Ingress 삭제
kubectl delete ingress phonebill-ingress -n phonebill

# 서비스 삭제
kubectl delete svc api-gateway user-service bill-service product-service kos-mock -n phonebill

# Deployment 삭제
kubectl delete deployment api-gateway user-service bill-service product-service kos-mock -n phonebill

# ConfigMap 삭제
kubectl delete configmap phonebill-common-config api-gateway-config user-service-config \
  bill-service-config product-service-config kos-mock-config -n phonebill

# Secret 삭제
kubectl delete secret phonebill-common-secret user-service-db-secret \
  bill-service-db-secret product-service-db-secret -n phonebill
```

---

## 원클릭 배포 스크립트

전체 배포를 한 번에 수행하는 스크립트:

```bash
#!/bin/bash
# deploy-all.sh

NAMESPACE=phonebill
REGISTRY=docker.io
PROJECT=hiondal
TAG=latest

echo "=== Minikube Backend 배포 시작 ==="

# 1. Namespace 생성
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# 2. Secrets 생성
echo "Creating secrets..."
kubectl create secret generic phonebill-common-secret \
  --namespace=$NAMESPACE \
  --from-literal=JWT_SECRET='your-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256' \
  --from-literal=REDIS_PASSWORD='' \
  --dry-run=client -o yaml | kubectl apply -f -

# 3. ConfigMaps 적용
echo "Applying ConfigMaps..."
kubectl apply -f deployment/k8s/configmaps/

# 4. Deployments 적용
echo "Applying Deployments..."
kubectl apply -f deployment/k8s/deployments/

# 5. Services 적용
echo "Applying Services..."
kubectl apply -f deployment/k8s/services/

# 6. Ingress 적용
echo "Applying Ingress..."
kubectl apply -f deployment/k8s/ingress/

# 7. 배포 상태 확인
echo "Waiting for deployments..."
kubectl rollout status deployment/api-gateway -n $NAMESPACE --timeout=120s
kubectl rollout status deployment/user-service -n $NAMESPACE --timeout=120s
kubectl rollout status deployment/bill-service -n $NAMESPACE --timeout=120s
kubectl rollout status deployment/product-service -n $NAMESPACE --timeout=120s
kubectl rollout status deployment/kos-mock -n $NAMESPACE --timeout=120s

echo "=== 배포 완료 ==="
kubectl get pods -n $NAMESPACE
```

---

## 작성자
- **작성자**: 최운영(데옵스)
- **작성일**: 2025-11-29
- **버전**: 1.0.0
