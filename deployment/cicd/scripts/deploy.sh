#!/bin/bash
set -e

ENVIRONMENT=${1:-dev}
IMAGE_TAG=${2:-latest}

echo "🚀 Starting deployment for environment: $ENVIRONMENT with image tag: $IMAGE_TAG"

# 환경별 이미지 태그 업데이트
cd deployment/cicd/kustomize/overlays/${ENVIRONMENT}

# 서비스 목록 (공백으로 구분)
services="api-gateway user-service bill-service product-service kos-mock"

echo "📦 Updating image tags for services: $services"

# 각 서비스 이미지 태그 업데이트
for service in $services; do
    echo "  - Updating $service to ${ENVIRONMENT}-${IMAGE_TAG}"
    kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/$service:${ENVIRONMENT}-${IMAGE_TAG}
done

echo "🔧 Applying Kubernetes manifests..."
# 배포 실행
kubectl apply -k .

echo "⏳ Waiting for deployments to be ready..."
# 배포 상태 확인
for service in $services; do
    echo "  - Checking rollout status for $service"
    kubectl rollout status deployment/$service -n phonebill-dg0500 --timeout=300s
done

echo "✅ Deployment completed successfully!"
echo ""
echo "📊 Current deployment status:"
kubectl get pods -n phonebill-dg0500 -o wide
echo ""
echo "🌐 Service endpoints:"
kubectl get services -n phonebill-dg0500
echo ""
echo "🔗 Ingress information:"
kubectl get ingress -n phonebill-dg0500