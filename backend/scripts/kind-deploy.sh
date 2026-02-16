#!/bin/bash
set -e

echo "=== Creating Kind cluster ==="
kind create cluster --config kind-config.yaml || echo "Cluster already exists"

echo "=== Building Docker images ==="
for svc in api-gateway auth-service user-service chat-service message-service media-service notification-service websocket-service; do
    echo "Building $svc..."
    docker build -t "whatsapp-$svc:latest" "./$svc"
done

echo "=== Loading images into Kind ==="
for svc in api-gateway auth-service user-service chat-service message-service media-service notification-service websocket-service; do
    kind load docker-image "whatsapp-$svc:latest" --name whatsapp
done

echo "=== Deploying with Helm ==="
helm upgrade --install whatsapp ./helm/whatsapp --create-namespace

echo "=== Waiting for pods to be ready ==="
kubectl -n whatsapp wait --for=condition=ready pod --all --timeout=300s || true

echo "=== Pod status ==="
kubectl -n whatsapp get pods

echo ""
echo "API Gateway accessible at: http://localhost:8080"
