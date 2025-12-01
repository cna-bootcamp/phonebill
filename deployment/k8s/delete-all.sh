#!/bin/bash
# Minikube 백엔드 서비스 삭제 스크립트
# 작성자: 최운영(데옵스)
# 작성일: 2025-11-29

set -e

NAMESPACE=phonebill
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=============================================="
echo "  PhoneBill Backend - 리소스 삭제"
echo "=============================================="
echo ""

read -p "⚠️  $NAMESPACE 네임스페이스의 모든 리소스를 삭제하시겠습니까? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "삭제를 취소합니다."
    exit 0
fi

echo ""
echo "[1/5] Ingress 삭제..."
kubectl delete -f "$SCRIPT_DIR/ingress/" --ignore-not-found=true || true

echo ""
echo "[2/5] Services 삭제..."
kubectl delete -f "$SCRIPT_DIR/services/" --ignore-not-found=true || true

echo ""
echo "[3/5] Deployments 삭제..."
kubectl delete -f "$SCRIPT_DIR/deployments/" --ignore-not-found=true || true

echo ""
echo "[4/5] ConfigMaps 삭제..."
kubectl delete -f "$SCRIPT_DIR/configmaps/" --ignore-not-found=true || true

echo ""
echo "[5/5] Secrets 삭제..."
kubectl delete -f "$SCRIPT_DIR/secrets/" --ignore-not-found=true || true

echo ""
echo "=============================================="
echo "  삭제 완료!"
echo "=============================================="
echo ""
echo "📦 남은 리소스 확인:"
kubectl get all -n $NAMESPACE 2>/dev/null || echo "리소스 없음"
echo ""
