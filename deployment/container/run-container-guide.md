# 백엔드 컨테이너 실행 가이드 (로컬 환경)

## 개요
본 문서는 phonebill 프로젝트의 백엔드 마이크로서비스들을 로컬 환경에서 Docker 컨테이너로 실행하는 방법을 안내합니다.

## 실행 정보
| 항목 | 값 |
|------|-----|
| Image Registry | docker.io |
| 시스템명 | phonebill |
| 실행 환경 | 로컬 (localhost) |

## 대상 서비스
| 서비스명 | 포트 | 설명 |
|---------|------|------|
| kos-mock | 8084 | KOS 목업 서비스 |
| user-service | 8081 | 사용자 인증/관리 서비스 |
| bill-service | 8082 | 요금 조회 서비스 |
| product-service | 8083 | 상품 변경 서비스 |
| api-gateway | 8080 | API Gateway |

## 사전 준비 사항

### 1. Docker 설치 확인
```bash
docker --version
```

### 2. 백킹 서비스 실행 확인
컨테이너 실행 전 아래 백킹 서비스가 실행 중이어야 합니다:
- **PostgreSQL** (auth-db: 15432, bill-inquiry-db: 25432, product-change-db: 35432)
- **Redis** (16379)

백킹 서비스 실행 상태 확인:
```bash
docker ps | grep -E "postgres|redis"
```

## 어플리케이션 빌드 및 컨테이너 이미지 생성

`deployment/container/build-image.md` 파일을 참조하여 이미지를 빌드합니다.

```bash
# Gradle 빌드
./gradlew clean build -x test

# 이미지 빌드 예시 (api-gateway)
docker build \
  --platform linux/amd64 \
  --build-arg BUILD_LIB_DIR="api-gateway/build/libs" \
  --build-arg ARTIFACTORY_FILE="api-gateway.jar" \
  -f deployment/container/Dockerfile-backend \
  -t api-gateway:latest .
```

## Docker Hub 로그인 (선택)

이미지를 Docker Hub에 푸시하려면 로그인이 필요합니다:
```bash
docker login docker.io -u {Docker Hub ID} -p {암호}
```

## 이미지 태그 및 푸시 (선택)

Docker Hub에 이미지를 푸시하는 경우:
```bash
# 태그
docker tag api-gateway:latest docker.io/hiondal/api-gateway:latest

# 푸시
docker push docker.io/hiondal/api-gateway:latest
```

---

## 컨테이너 실행

> **중요**: 서비스 간 의존성이 있으므로 아래 순서대로 실행하세요.
> 1. kos-mock → 2. user-service → 3. bill-service → 4. product-service → 5. api-gateway

### 1. kos-mock 실행
```bash
docker run -d --name kos-mock --rm \
  -p 8084:8084 \
  -e SERVER_PORT=8084 \
  -e SPRING_PROFILES_ACTIVE=dev \
  kos-mock:latest
```

### 2. user-service 실행
```bash
docker run -d --name user-service --rm \
  -p 8081:8081 \
  -e SERVER_PORT=8081 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e CORS_ALLOWED_ORIGINS=http://localhost:3000 \
  -e DB_HOST=localhost \
  -e DB_PORT=15432 \
  -e DB_NAME=authdb \
  -e DB_USERNAME=unicorn \
  -e DB_PASSWORD='P@ssw0rd$' \
  -e DB_KIND=postgresql \
  -e DDL_AUTO=update \
  -e SHOW_SQL=true \
  -e REDIS_HOST=host.docker.internal \
  -e REDIS_PORT=16379 \
  -e REDIS_PASSWORD='P@ssw0rd$' \
  -e REDIS_DATABASE=0 \
  -e JWT_SECRET='nwe5Yo9qaJ6FBD/Thl2/j6/SFAfNwUorAY1ZcWO2KI7uA4bmVLOCPxE9hYuUpRCOkgV2UF2DdHXtqHi3+BU/ecbz2zpHyf/720h48UbA3XOMYOX1sdM+dQ==' \
  -e JWT_ACCESS_TOKEN_VALIDITY=18000000 \
  -e JWT_REFRESH_TOKEN_VALIDITY=86400000 \
  user-service:latest
```

### 3. bill-service 실행
```bash
docker run -d --name bill-service --rm \
  -p 8082:8082 \
  -e SERVER_PORT=8082 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e CORS_ALLOWED_ORIGINS=http://localhost:3000 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=25432 \
  -e DB_NAME=inquirydb \
  -e DB_USERNAME=unicorn \
  -e DB_PASSWORD='P@ssw0rd$' \
  -e DB_KIND=postgresql \
  -e DB_MAX_POOL=20 \
  -e DB_MIN_IDLE=5 \
  -e DB_CONNECTION_TIMEOUT=30000 \
  -e DB_IDLE_TIMEOUT=600000 \
  -e DB_MAX_LIFETIME=1800000 \
  -e DB_LEAK_DETECTION=60000 \
  -e REDIS_HOST=host.docker.internal \
  -e REDIS_PORT=16379 \
  -e REDIS_PASSWORD='P@ssw0rd$' \
  -e REDIS_DATABASE=1 \
  -e REDIS_TIMEOUT=2000 \
  -e REDIS_MAX_ACTIVE=8 \
  -e REDIS_MAX_IDLE=8 \
  -e REDIS_MIN_IDLE=0 \
  -e REDIS_MAX_WAIT=-1 \
  -e KOS_BASE_URL=http://host.docker.internal:8084 \
  -e JWT_SECRET='nwe5Yo9qaJ6FBD/Thl2/j6/SFAfNwUorAY1ZcWO2KI7uA4bmVLOCPxE9hYuUpRCOkgV2UF2DdHXtqHi3+BU/ecbz2zpHyf/720h48UbA3XOMYOX1sdM+dQ==' \
  -e JWT_ACCESS_TOKEN_VALIDITY=18000000 \
  -e JWT_REFRESH_TOKEN_VALIDITY=86400000 \
  -e LOG_FILE_NAME=logs/bill-service.log \
  bill-service:latest
```

### 4. product-service 실행
```bash
docker run -d --name product-service --rm \
  -p 8083:8083 \
  -e SERVER_PORT=8083 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e CORS_ALLOWED_ORIGINS=http://localhost:3000 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=35432 \
  -e DB_NAME=changedb \
  -e DB_USERNAME=unicorn \
  -e DB_PASSWORD='P@ssw0rd$' \
  -e DB_KIND=postgresql \
  -e DDL_AUTO=update \
  -e REDIS_HOST=host.docker.internal \
  -e REDIS_PORT=16379 \
  -e REDIS_PASSWORD='P@ssw0rd$' \
  -e REDIS_DATABASE=2 \
  -e KOS_BASE_URL=http://host.docker.internal:8084 \
  -e KOS_MOCK_ENABLED=true \
  -e KOS_CLIENT_ID=product-service-dev \
  -e KOS_API_KEY=dev-api-key \
  -e JWT_SECRET='nwe5Yo9qaJ6FBD/Thl2/j6/SFAfNwUorAY1ZcWO2KI7uA4bmVLOCPxE9hYuUpRCOkgV2UF2DdHXtqHi3+BU/ecbz2zpHyf/720h48UbA3XOMYOX1sdM+dQ==' \
  -e JWT_ACCESS_TOKEN_VALIDITY=18000000 \
  -e JWT_REFRESH_TOKEN_VALIDITY=86400000 \
  product-service:latest
```

### 5. api-gateway 실행
```bash
docker run -d --name api-gateway --rm \
  -p 8080:8080 \
  -e SERVER_PORT=8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e CORS_ALLOWED_ORIGINS=http://localhost:3000 \
  -e USER_SERVICE_URL=http://host.docker.internal:8081 \
  -e BILL_SERVICE_URL=http://host.docker.internal:8082 \
  -e PRODUCT_SERVICE_URL=http://host.docker.internal:8083 \
  -e KOS_MOCK_URL=http://host.docker.internal:8084 \
  -e JWT_SECRET='nwe5Yo9qaJ6FBD/Thl2/j6/SFAfNwUorAY1ZcWO2KI7uA4bmVLOCPxE9hYuUpRCOkgV2UF2DdHXtqHi3+BU/ecbz2zpHyf/720h48UbA3XOMYOX1sdM+dQ==' \
  -e JWT_ACCESS_TOKEN_VALIDITY=18000000 \
  -e JWT_REFRESH_TOKEN_VALIDITY=86400000 \
  api-gateway:latest
```

---

## 실행 확인

### 컨테이너 실행 상태 확인
```bash
docker ps | grep -E "api-gateway|user-service|bill-service|product-service|kos-mock"
```

### 예상 결과
```
CONTAINER ID   IMAGE                  PORTS                    NAMES
xxxxxxxxxxxx   api-gateway:latest     0.0.0.0:8080->8080/tcp   api-gateway
xxxxxxxxxxxx   product-service:latest 0.0.0.0:8083->8083/tcp   product-service
xxxxxxxxxxxx   bill-service:latest    0.0.0.0:8082->8082/tcp   bill-service
xxxxxxxxxxxx   user-service:latest    0.0.0.0:8081->8081/tcp   user-service
xxxxxxxxxxxx   kos-mock:latest        0.0.0.0:8084->8084/tcp   kos-mock
```

### 서비스 헬스체크
```bash
# API Gateway
curl http://localhost:8080/actuator/health

# User Service
curl http://localhost:8081/actuator/health

# Bill Service
curl http://localhost:8082/actuator/health

# Product Service
curl http://localhost:8083/actuator/health

# KOS Mock
curl http://localhost:8084/actuator/health
```

### 로그 확인
```bash
docker logs -f api-gateway
docker logs -f user-service
docker logs -f bill-service
docker logs -f product-service
docker logs -f kos-mock
```

---

## 컨테이너 중지

### 개별 서비스 중지
```bash
docker stop api-gateway
docker stop product-service
docker stop bill-service
docker stop user-service
docker stop kos-mock
```

### 전체 서비스 중지
```bash
docker stop api-gateway product-service bill-service user-service kos-mock
```

---

## 재배포 방법

### 1. 소스 수정 후 로컬에서 푸시
```bash
git add .
git commit -m "수정 내용"
git push
```

### 2. 소스 내려받기
```bash
cd ~/home/workspace/phonebill
git pull
```

### 3. 컨테이너 이미지 재생성
`deployment/container/build-image.md` 파일을 참조하여 이미지를 다시 빌드합니다.

### 4. 이미지 푸시 (Docker Hub 사용 시)
```bash
docker tag api-gateway:latest docker.io/phonebill/api-gateway:latest
docker push docker.io/phonebill/api-gateway:latest
```

### 5. 컨테이너 중지
```bash
docker stop api-gateway
```

### 6. 기존 이미지 삭제 (선택)
```bash
docker rmi api-gateway:latest
```

### 7. 컨테이너 재실행
위의 "컨테이너 실행" 섹션의 명령어를 다시 실행합니다.

---

## 문제 해결

### 1. 컨테이너가 시작되지 않는 경우
```bash
# 로그 확인
docker logs api-gateway

# 컨테이너 상태 확인
docker ps -a | grep api-gateway
```

### 2. 서비스 간 통신 오류
- `host.docker.internal`이 올바르게 설정되었는지 확인
- 방화벽 설정 확인
- 백킹 서비스(DB, Redis) 실행 상태 확인

### 3. 포트 충돌
```bash
# 사용 중인 포트 확인
lsof -i :8080
```

---

## 작성일
2025-11-27
