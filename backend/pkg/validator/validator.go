package validator

import (
	"regexp"

	"github.com/google/uuid"
)

var e164Regex = regexp.MustCompile(`^\+[1-9]\d{1,14}$`)

func IsValidPhone(phone string) bool {
	return e164Regex.MatchString(phone)
}

func IsValidUUID(s string) bool {
	_, err := uuid.Parse(s)
	return err == nil
}

func ValidatePhone(phone string) error {
	if !IsValidPhone(phone) {
		return &ValidationError{Field: "phone", Message: "must be valid E.164 format (e.g. +14155552671)"}
	}
	return nil
}

func ValidateUUID(s, fieldName string) error {
	if !IsValidUUID(s) {
		return &ValidationError{Field: fieldName, Message: "must be a valid UUID"}
	}
	return nil
}

func ValidatePageSize(size, max int) int {
	if size <= 0 {
		return 50
	}
	if size > max {
		return max
	}
	return size
}

type ValidationError struct {
	Field   string `json:"field"`
	Message string `json:"message"`
}

func (e *ValidationError) Error() string {
	return e.Field + ": " + e.Message
}
