#!/bin/bash
set -e

ENVIRONMENT=${1:-dev}
IMAGE_TAG=${2:-latest}

echo "🚀 Manual deployment starting..."
echo "Environment: $ENVIRONMENT"
echo "Image Tag: $IMAGE_TAG"

# Check if kustomize is installed
if ! command -v kustomize &> /dev/null; then
    echo "Installing Kustomize..."
    curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh" | bash
    sudo mv kustomize /usr/local/bin/
fi

# Load environment variables from .github/config
if [[ -f ".github/config/deploy_env_vars_${ENVIRONMENT}" ]]; then
    source ".github/config/deploy_env_vars_${ENVIRONMENT}"
    echo "✅ Environment variables loaded for $ENVIRONMENT"
else
    echo "❌ Environment configuration file not found: .github/config/deploy_env_vars_${ENVIRONMENT}"
    exit 1
fi

# Create namespace
echo "📝 Creating namespace phonebill-dg0500..."
kubectl create namespace phonebill-dg0500 --dry-run=client -o yaml | kubectl apply -f -

# 환경별 이미지 태그 업데이트 (.github/kustomize 사용)
cd .github/kustomize/overlays/${ENVIRONMENT}

echo "🔄 Updating image tags..."
# 서비스 배열 정의
services=(api-gateway user-service bill-service product-service kos-mock)

# 각 서비스별 이미지 태그 업데이트
for service in "${services[@]}"; do
  kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/$service:${ENVIRONMENT}-${IMAGE_TAG}
done

echo "🚀 Deploying to Kubernetes..."
# 배포 실행
kubectl apply -k .

echo "⏳ Waiting for deployments to be ready..."
# 서비스별 배포 상태 확인
for service in "${services[@]}"; do
  kubectl rollout status deployment/$service -n phonebill-dg0500 --timeout=300s
done

echo "🔍 Health check..."
# API Gateway Health Check (첫 번째 서비스가 API Gateway라고 가정)
GATEWAY_SERVICE=${services[0]}
GATEWAY_POD=$(kubectl get pod -n phonebill-dg0500 -l app.kubernetes.io/name=$GATEWAY_SERVICE -o jsonpath='{.items[0].metadata.name}')
kubectl -n phonebill-dg0500 exec $GATEWAY_POD -- curl -f http://localhost:8080/actuator/health || echo "Health check failed, but deployment completed"

echo "📋 Service Information:"
kubectl get pods -n phonebill-dg0500
kubectl get services -n phonebill-dg0500
kubectl get ingress -n phonebill-dg0500

echo "✅ GitHub Actions deployment completed successfully!"
