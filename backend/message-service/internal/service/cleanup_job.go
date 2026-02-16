package service

import (
	"context"
	"time"

	"github.com/rs/zerolog"

	"github.com/whatsapp-clone/backend/message-service/internal/repository"
)

// DisappearingMessagesCleaner runs a periodic job to delete expired messages.
type DisappearingMessagesCleaner struct {
	repo     repository.MessageRepository
	interval time.Duration
	log      zerolog.Logger
	stopCh   chan struct{}
}

// NewDisappearingMessagesCleaner creates a new cleaner with the given check interval.
func NewDisappearingMessagesCleaner(repo repository.MessageRepository, interval time.Duration, log zerolog.Logger) *DisappearingMessagesCleaner {
	return &DisappearingMessagesCleaner{
		repo:     repo,
		interval: interval,
		log:      log.With().Str("component", "disappearing-cleaner").Logger(),
		stopCh:   make(chan struct{}),
	}
}

// Start begins the cleanup loop in a goroutine.
func (c *DisappearingMessagesCleaner) Start(ctx context.Context) {
	go func() {
		ticker := time.NewTicker(c.interval)
		defer ticker.Stop()

		c.log.Info().Dur("interval", c.interval).Msg("disappearing messages cleaner started")

		for {
			select {
			case <-ticker.C:
				c.runCleanup(ctx)
			case <-c.stopCh:
				c.log.Info().Msg("disappearing messages cleaner stopped")
				return
			case <-ctx.Done():
				c.log.Info().Msg("disappearing messages cleaner context cancelled")
				return
			}
		}
	}()
}

// Stop signals the cleanup loop to exit.
func (c *DisappearingMessagesCleaner) Stop() {
	close(c.stopCh)
}

func (c *DisappearingMessagesCleaner) runCleanup(ctx context.Context) {
	// Standard disappearing message thresholds matching WhatsApp options
	thresholds := []time.Duration{
		24 * time.Hour,      // 24h
		7 * 24 * time.Hour,  // 7d
		90 * 24 * time.Hour, // 90d
	}

	totalDeleted := int64(0)
	for _, threshold := range thresholds {
		cutoff := time.Now().Add(-threshold)
		count, err := c.repo.DeleteExpiredMessages(ctx, cutoff)
		if err != nil {
			c.log.Error().Err(err).Dur("threshold", threshold).Msg("failed to delete expired messages")
			continue
		}
		totalDeleted += count
	}

	if totalDeleted > 0 {
		c.log.Info().Int64("deleted", totalDeleted).Msg("cleaned up disappearing messages")
	}
}
