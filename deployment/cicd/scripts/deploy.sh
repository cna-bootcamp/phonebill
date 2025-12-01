#!/bin/bash
set -e

ENVIRONMENT=${1:-dev}
IMAGE_TAG=${2:-latest}

echo "🚀 Deploying to ${ENVIRONMENT} environment with tag ${IMAGE_TAG}..."

# 환경별 설정 파일 로드
source "$(dirname "$0")/../config/deploy_env_vars_${ENVIRONMENT}"

# 환경별 이미지 태그 업데이트
cd "$(dirname "$0")/../kustomize/overlays/${ENVIRONMENT}"

# 서비스 목록
services="api-gateway user-service bill-service product-service kos-mock"

# 각 서비스 이미지 태그 업데이트
for service in $services; do
    echo "📦 Updating image tag for ${service}..."
    kustomize edit set image docker.io/hiondal/$service:${ENVIRONMENT}-${IMAGE_TAG}
done

# 배포 실행
echo "📋 Applying Kustomize manifests..."
kubectl apply -k .

# 배포 상태 확인
echo "⏳ Waiting for deployments to be ready..."
for service in $services; do
    echo "  Checking ${service}..."
    kubectl rollout status deployment/$service -n ${namespace} --timeout=300s || echo "  ⚠️ Timeout waiting for ${service}"
done

echo "✅ Deployment completed successfully!"
echo ""
echo "📊 Current status:"
kubectl get pods -n ${namespace}
