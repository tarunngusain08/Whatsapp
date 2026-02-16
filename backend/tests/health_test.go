package tests

import (
	"net/http"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestHealthCheck_Liveness(t *testing.T) {
	client := &http.Client{Timeout: 5 * time.Second}

	resp, err := client.Get(baseURL + "/health")
	require.NoError(t, err)
	defer resp.Body.Close()

	assert.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.Equal(t, "alive", body["status"])
}

func TestHealthCheck_Readiness(t *testing.T) {
	client := &http.Client{Timeout: 5 * time.Second}

	resp, err := client.Get(baseURL + "/ready")
	require.NoError(t, err)
	defer resp.Body.Close()

	assert.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	status := body["status"].(map[string]interface{})
	for name, val := range status {
		assert.Equal(t, "ok", val, "readiness check for %s should be ok", name)
	}
}
