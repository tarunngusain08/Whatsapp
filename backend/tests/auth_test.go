package tests

import (
	"net/http"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestAuthFlow_RequestOTP(t *testing.T) {
	resp := doRequest(t, "POST", "/api/v1/auth/request-otp", map[string]string{
		"phone": "+14155551001",
	}, "")
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.True(t, body["success"].(bool))

	data := body["data"].(map[string]interface{})
	assert.NotEmpty(t, data["otp"], "OTP should be returned in dev mode")
	assert.NotEmpty(t, data["message"])
}

func TestAuthFlow_RequestOTP_MissingPhone(t *testing.T) {
	resp := doRequest(t, "POST", "/api/v1/auth/request-otp", map[string]string{}, "")
	// The binding:"required" tag on phone should reject this
	assert.NotEqual(t, http.StatusOK, resp.StatusCode, "missing phone should not return 200")
	_ = parseResponseRaw(t, resp)
}

func TestAuthFlow_VerifyOTP_Success(t *testing.T) {
	phone := "+14155551002"
	token, refresh, userID := registerUser(t, phone)
	assert.NotEmpty(t, token)
	assert.NotEmpty(t, refresh)
	assert.NotEmpty(t, userID)
}

func TestAuthFlow_VerifyOTP_WrongCode(t *testing.T) {
	phone := "+14155551003"

	// Request OTP first
	resp := doRequest(t, "POST", "/api/v1/auth/request-otp", map[string]string{
		"phone": phone,
	}, "")
	require.Equal(t, http.StatusOK, resp.StatusCode)
	_ = parseResponse(t, resp)

	// Verify with wrong code
	resp = doRequest(t, "POST", "/api/v1/auth/verify-otp", map[string]string{
		"phone": phone,
		"code":  "000000",
	}, "")
	assert.Equal(t, http.StatusBadRequest, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.False(t, body["success"].(bool))
	errBody := body["error"].(map[string]interface{})
	assert.Equal(t, "OTP_INVALID", errBody["code"])
}

func TestAuthFlow_VerifyOTP_Expired(t *testing.T) {
	// Try to verify an OTP that was never requested
	resp := doRequest(t, "POST", "/api/v1/auth/verify-otp", map[string]string{
		"phone": "+14155551099",
		"code":  "123456",
	}, "")
	assert.Equal(t, http.StatusBadRequest, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.False(t, body["success"].(bool))
	errBody := body["error"].(map[string]interface{})
	assert.Equal(t, "OTP_EXPIRED", errBody["code"])
}

func TestAuthFlow_RefreshToken(t *testing.T) {
	_, refreshToken, _ := registerUser(t, "+14155551004")

	resp := doRequest(t, "POST", "/api/v1/auth/refresh", map[string]string{
		"refresh_token": refreshToken,
	}, "")
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.True(t, body["success"].(bool))
	data := body["data"].(map[string]interface{})
	assert.NotEmpty(t, data["access_token"])
	assert.NotEmpty(t, data["refresh_token"])
}

func TestAuthFlow_RefreshToken_Invalid(t *testing.T) {
	resp := doRequest(t, "POST", "/api/v1/auth/refresh", map[string]string{
		"refresh_token": "totally-invalid-token",
	}, "")
	assert.Equal(t, http.StatusUnauthorized, resp.StatusCode)
}

func TestAuthFlow_RefreshToken_Rotation(t *testing.T) {
	// After refresh, the old refresh token should be revoked (rotation).
	_, refreshToken, _ := registerUser(t, "+14155551010")

	// Refresh once to get new tokens
	resp := doRequest(t, "POST", "/api/v1/auth/refresh", map[string]string{
		"refresh_token": refreshToken,
	}, "")
	require.Equal(t, http.StatusOK, resp.StatusCode)

	// Try using the old refresh token again — should fail
	resp = doRequest(t, "POST", "/api/v1/auth/refresh", map[string]string{
		"refresh_token": refreshToken,
	}, "")
	assert.Equal(t, http.StatusUnauthorized, resp.StatusCode, "old refresh token should be revoked after rotation")
}

func TestAuthFlow_Logout(t *testing.T) {
	_, refreshToken, _ := registerUser(t, "+14155551005")

	resp := doRequest(t, "POST", "/api/v1/auth/logout", map[string]string{
		"refresh_token": refreshToken,
	}, "")
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	// Refresh should fail after logout
	resp = doRequest(t, "POST", "/api/v1/auth/refresh", map[string]string{
		"refresh_token": refreshToken,
	}, "")
	assert.Equal(t, http.StatusUnauthorized, resp.StatusCode, "refresh after logout should be rejected")
}

func TestAuthFlow_ProtectedRoute_NoToken(t *testing.T) {
	resp := doRequest(t, "GET", "/api/v1/users/me", nil, "")
	assert.Equal(t, http.StatusUnauthorized, resp.StatusCode)
	_ = parseResponseRaw(t, resp)
}

func TestAuthFlow_ProtectedRoute_InvalidToken(t *testing.T) {
	resp := doRequest(t, "GET", "/api/v1/users/me", nil, "invalid-jwt-token")
	assert.Equal(t, http.StatusUnauthorized, resp.StatusCode)
	_ = parseResponseRaw(t, resp)
}

func TestAuthFlow_ProtectedRoute_ValidToken(t *testing.T) {
	token, _, _ := registerUser(t, "+14155551006")

	resp := doRequest(t, "GET", "/api/v1/users/me", nil, token)
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.True(t, body["success"].(bool))
}
