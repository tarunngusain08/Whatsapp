package errors

import "fmt"

const (
	CodeInternal         = "INTERNAL_ERROR"
	CodeNotFound         = "NOT_FOUND"
	CodeBadRequest       = "BAD_REQUEST"
	CodeUnauthorized     = "UNAUTHORIZED"
	CodeForbidden        = "FORBIDDEN"
	CodeConflict         = "CONFLICT"
	CodeTooManyRequests  = "TOO_MANY_REQUESTS"
	CodeValidation       = "VALIDATION_ERROR"
	CodeOTPExpired       = "OTP_EXPIRED"
	CodeOTPInvalid       = "OTP_INVALID"
	CodeOTPMaxAttempts   = "OTP_MAX_ATTEMPTS"
	CodeTokenExpired     = "TOKEN_EXPIRED"
	CodeTokenInvalid     = "TOKEN_INVALID"
	CodeMediaTooLarge    = "MEDIA_TOO_LARGE"
	CodeMediaInvalidType = "MEDIA_INVALID_TYPE"
	CodeChatNotFound     = "CHAT_NOT_FOUND"
	CodeNotChatMember    = "NOT_CHAT_MEMBER"
	CodeNotAdmin         = "NOT_ADMIN"
	CodeAlreadyMember    = "ALREADY_MEMBER"
	CodeUserBlocked      = "USER_BLOCKED"
)

type AppError struct {
	Code       string `json:"code"`
	Message    string `json:"message"`
	HTTPStatus int    `json:"-"`
	Err        error  `json:"-"`
}

func (e *AppError) Error() string {
	if e.Err != nil {
		return fmt.Sprintf("%s: %s: %v", e.Code, e.Message, e.Err)
	}
	return fmt.Sprintf("%s: %s", e.Code, e.Message)
}

func (e *AppError) Unwrap() error {
	return e.Err
}

func NewInternal(msg string, err error) *AppError {
	return &AppError{Code: CodeInternal, Message: msg, HTTPStatus: 500, Err: err}
}

func NewNotFound(msg string) *AppError {
	return &AppError{Code: CodeNotFound, Message: msg, HTTPStatus: 404}
}

func NewBadRequest(msg string) *AppError {
	return &AppError{Code: CodeBadRequest, Message: msg, HTTPStatus: 400}
}

func NewUnauthorized(msg string) *AppError {
	return &AppError{Code: CodeUnauthorized, Message: msg, HTTPStatus: 401}
}

func NewForbidden(msg string) *AppError {
	return &AppError{Code: CodeForbidden, Message: msg, HTTPStatus: 403}
}

func NewConflict(msg string) *AppError {
	return &AppError{Code: CodeConflict, Message: msg, HTTPStatus: 409}
}

func NewTooManyRequests(msg string) *AppError {
	return &AppError{Code: CodeTooManyRequests, Message: msg, HTTPStatus: 429}
}

func NewValidation(msg string) *AppError {
	return &AppError{Code: CodeValidation, Message: msg, HTTPStatus: 422}
}

func Wrap(code string, httpStatus int, msg string, err error) *AppError {
	return &AppError{Code: code, Message: msg, HTTPStatus: httpStatus, Err: err}
}
