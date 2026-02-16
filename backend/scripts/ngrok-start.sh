#!/bin/bash
set -e

PORT=${1:-8080}
NGROK_DOMAIN=${NGROK_DOMAIN:-}

echo "Starting ngrok tunnel on port $PORT..."

if [ -n "$NGROK_DOMAIN" ]; then
    echo "Using static domain: $NGROK_DOMAIN"
    ngrok http "$PORT" --domain "$NGROK_DOMAIN" &
else
    ngrok http "$PORT" &
fi

NGROK_PID=$!

# Wait for ngrok to start
sleep 3

# Get the public URL
PUBLIC_URL=$(curl -s http://localhost:4040/api/tunnels | python3 -c "import sys,json; tunnels=json.load(sys.stdin)['tunnels']; print(tunnels[0]['public_url'] if tunnels else 'ERROR')" 2>/dev/null)

if [ "$PUBLIC_URL" = "ERROR" ] || [ -z "$PUBLIC_URL" ]; then
    echo "ERROR: Failed to get ngrok public URL"
    kill $NGROK_PID 2>/dev/null
    exit 1
fi

echo ""
echo "============================================"
echo "  WhatsApp Backend Public URL"
echo "============================================"
echo "  URL: $PUBLIC_URL"
echo "  API: $PUBLIC_URL/api/v1"
echo "  WS:  ${PUBLIC_URL/https/wss}/ws"
echo "============================================"
echo ""
echo "  Health check: $PUBLIC_URL/health"
echo ""

# Generate QR code if qrencode is available
if command -v qrencode &> /dev/null; then
    echo "QR Code:"
    qrencode -t ANSIUTF8 "$PUBLIC_URL"
fi

echo "Press Ctrl+C to stop the tunnel"
wait $NGROK_PID
