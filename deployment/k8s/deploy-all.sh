#!/bin/bash
# Minikube 백엔드 서비스 원클릭 배포 스크립트
# 작성자: 최운영(데옵스)
# 작성일: 2025-11-29

set -e

NAMESPACE=phonebill
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=============================================="
echo "  PhoneBill Backend - Minikube 배포 시작"
echo "=============================================="

# 1. Kubernetes Context 확인
echo ""
echo "[1/7] Kubernetes Context 확인..."
CURRENT_CONTEXT=$(kubectl config current-context)
echo "현재 Context: $CURRENT_CONTEXT"

if [[ "$CURRENT_CONTEXT" != "minikube-remote" ]]; then
    echo "⚠️  경고: 현재 Context가 minikube-remote가 아닙니다."
    read -p "계속 진행하시겠습니까? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "배포를 취소합니다."
        exit 1
    fi
fi

# 2. Namespace 생성
echo ""
echo "[2/7] Namespace 생성..."
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -
kubectl config set-context --current --namespace=$NAMESPACE

# 3. Secrets 적용
echo ""
echo "[3/7] Secrets 적용..."
kubectl apply -f "$SCRIPT_DIR/secrets/"

# 4. ConfigMaps 적용
echo ""
echo "[4/7] ConfigMaps 적용..."
kubectl apply -f "$SCRIPT_DIR/configmaps/"

# 5. Deployments 적용
echo ""
echo "[5/7] Deployments 적용..."
kubectl apply -f "$SCRIPT_DIR/deployments/"

# 6. Services 적용
echo ""
echo "[6/7] Services 적용..."
kubectl apply -f "$SCRIPT_DIR/services/"

# 7. Ingress 적용
echo ""
echo "[7/7] Ingress 적용..."
kubectl apply -f "$SCRIPT_DIR/ingress/"

# 배포 상태 대기
echo ""
echo "=============================================="
echo "  배포 상태 확인 중..."
echo "=============================================="

for service in api-gateway user-service bill-service product-service kos-mock; do
    echo ""
    echo "⏳ $service 배포 대기 중..."
    kubectl rollout status deployment/$service -n $NAMESPACE --timeout=180s || {
        echo "❌ $service 배포 실패"
        kubectl describe deployment/$service -n $NAMESPACE
        exit 1
    }
    echo "✅ $service 배포 완료"
done

# 최종 상태 출력
echo ""
echo "=============================================="
echo "  배포 완료!"
echo "=============================================="
echo ""
echo "📦 Pods 상태:"
kubectl get pods -n $NAMESPACE
echo ""
echo "🔗 Services:"
kubectl get svc -n $NAMESPACE
echo ""
echo "🌐 Ingress:"
kubectl get ingress -n $NAMESPACE
echo ""
echo "=============================================="
echo "  접속 방법"
echo "=============================================="
echo ""
echo "API Gateway Ingress Host:"
echo "   phonebill-api.72.155.72.236.nip.io"
echo ""
echo "Health Check:"
echo "   curl http://phonebill-api.72.155.72.236.nip.io/actuator/health"
echo ""
