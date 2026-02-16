package model

type OTPEntry struct {
	HashedOTP string `json:"hashed_otp"`
	Attempts  int    `json:"attempts"`
}
