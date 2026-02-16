package service

import (
	"encoding/json"

	"github.com/nats-io/nats.go"
	"github.com/rs/zerolog"
)

type eventPublisher struct {
	js  nats.JetStreamContext
	log zerolog.Logger
}

func (p *eventPublisher) publishEvent(subject string, payload interface{}) {
	data, err := json.Marshal(payload)
	if err != nil {
		p.log.Error().Err(err).Str("subject", subject).Msg("failed to marshal NATS event")
		return
	}
	if _, err := p.js.Publish(subject, data); err != nil {
		p.log.Error().Err(err).Str("subject", subject).Msg("failed to publish NATS event")
	}
}
