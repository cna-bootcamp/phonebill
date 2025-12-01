# 백엔드 컨테이너 이미지 작성 결과서

## 개요
본 문서는 phonebill 프로젝트의 백엔드 마이크로서비스들의 컨테이너 이미지 빌드 과정과 결과를 기록합니다.

## 대상 서비스
| 서비스명 | 설명 |
|---------|------|
| api-gateway | API Gateway 서비스 |
| user-service | 사용자 인증/관리 서비스 |
| bill-service | 요금 조회 서비스 |
| product-service | 상품 변경 서비스 |
| kos-mock | KOS 목업 서비스 |

## Dockerfile 구성

**파일 위치**: `deployment/container/Dockerfile-backend`

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk AS builder
ARG BUILD_LIB_DIR
ARG ARTIFACTORY_FILE
COPY ${BUILD_LIB_DIR}/${ARTIFACTORY_FILE} app.jar

# Run stage
FROM eclipse-temurin:21-jre-alpine
ENV USERNAME=k8s
ENV ARTIFACTORY_HOME=/home/${USERNAME}
ENV JAVA_OPTS=""

# Add a non-root user
RUN addgroup -S ${USERNAME} && \
    adduser -S -G ${USERNAME} ${USERNAME} && \
    mkdir -p ${ARTIFACTORY_HOME} && \
    chown ${USERNAME}:${USERNAME} ${ARTIFACTORY_HOME}

WORKDIR ${ARTIFACTORY_HOME}
COPY --from=builder app.jar app.jar
RUN chown ${USERNAME}:${USERNAME} app.jar

USER ${USERNAME}

ENTRYPOINT [ "sh", "-c" ]
CMD ["java ${JAVA_OPTS} -jar app.jar"]
```

### Dockerfile 특징
- **멀티스테이지 빌드**: 빌드와 실행 환경 분리로 이미지 크기 최적화
- **베이스 이미지**: Eclipse Temurin 21 JRE Alpine (경량화)
- **보안**: 비루트 사용자(k8s)로 실행
- **유연성**: `JAVA_OPTS` 환경변수로 JVM 옵션 설정 가능

## 빌드 과정

### 1. Gradle 빌드 (JAR 파일 생성)
```bash
./gradlew clean build -x test
```

### 2. 컨테이너 이미지 빌드
각 서비스별로 아래 명령을 실행합니다:

```bash
DOCKER_FILE=deployment/container/Dockerfile-backend

# api-gateway
docker build \
  --platform linux/amd64 \
  --build-arg BUILD_LIB_DIR="api-gateway/build/libs" \
  --build-arg ARTIFACTORY_FILE="api-gateway.jar" \
  -f ${DOCKER_FILE} \
  -t api-gateway:latest .

# user-service
docker build \
  --platform linux/amd64 \
  --build-arg BUILD_LIB_DIR="user-service/build/libs" \
  --build-arg ARTIFACTORY_FILE="user-service.jar" \
  -f ${DOCKER_FILE} \
  -t user-service:latest .

# bill-service
docker build \
  --platform linux/amd64 \
  --build-arg BUILD_LIB_DIR="bill-service/build/libs" \
  --build-arg ARTIFACTORY_FILE="bill-service.jar" \
  -f ${DOCKER_FILE} \
  -t bill-service:latest .

# product-service
docker build \
  --platform linux/amd64 \
  --build-arg BUILD_LIB_DIR="product-service/build/libs" \
  --build-arg ARTIFACTORY_FILE="product-service.jar" \
  -f ${DOCKER_FILE} \
  -t product-service:latest .

# kos-mock
docker build \
  --platform linux/amd64 \
  --build-arg BUILD_LIB_DIR="kos-mock/build/libs" \
  --build-arg ARTIFACTORY_FILE="kos-mock.jar" \
  -f ${DOCKER_FILE} \
  -t kos-mock:latest .
```

## 빌드 결과

### 생성된 이미지 목록
| 이미지명 | 태그 | 크기 | 상태 |
|---------|------|------|------|
| api-gateway | latest | 158MB | ✅ 성공 |
| user-service | latest | 205MB | ✅ 성공 |
| bill-service | latest | 214MB | ✅ 성공 |
| product-service | latest | 220MB | ✅ 성공 |
| kos-mock | latest | 201MB | ✅ 성공 |

### 이미지 확인 명령
```bash
docker images | grep -E "api-gateway|user-service|bill-service|product-service|kos-mock"
```

## 이미지 레지스트리 푸시 (선택)

Azure Container Registry에 푸시하는 경우:
```bash
REGISTRY=docker.io
PROJECT=hiondal
TAG=latest

# 로그인
az acr login --name acrdigitalgarage01

# 태그 및 푸시
for service in api-gateway user-service bill-service product-service kos-mock; do
  docker tag ${service}:latest ${REGISTRY}/${PROJECT}/${service}:${TAG}
  docker push ${REGISTRY}/${PROJECT}/${service}:${TAG}
done
```

## 작성일
2025-11-27
