#!/bin/bash
set -e

echo "Waiting for MinIO to be ready..."
until mc alias set myminio http://minio:9000 minioadmin minioadmin 2>/dev/null; do
    echo "MinIO not ready, retrying in 2s..."
    sleep 2
done

echo "Creating whatsapp-media bucket..."
mc mb myminio/whatsapp-media --ignore-existing
mc anonymous set download myminio/whatsapp-media

echo "MinIO bucket initialized successfully"
