#!/bin/bash
set -e
echo "=== Deleting Kind cluster ==="
kind delete cluster --name whatsapp
echo "Cluster deleted."
