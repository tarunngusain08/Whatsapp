package service

import (
	"context"
	"time"
)

// StartCleanupJob runs a periodic goroutine that finds and removes orphaned media
// (media not referenced by any message, older than 24 hours).
func (s *mediaServiceImpl) StartCleanupJob(ctx context.Context) {
	ticker := time.NewTicker(s.cfg.CleanupInterval)
	go func() {
		for {
			select {
			case <-ctx.Done():
				ticker.Stop()
				return
			case <-ticker.C:
				s.runCleanup(ctx)
			}
		}
	}()
}

func (s *mediaServiceImpl) runCleanup(ctx context.Context) {
	s.log.Info().Msg("starting orphaned media cleanup")

	cutoff := time.Now().Add(-24 * time.Hour)
	orphaned, err := s.mediaRepo.FindOrphaned(ctx, cutoff)
	if err != nil {
		s.log.Error().Err(err).Msg("failed to find orphaned media")
		return
	}

	for _, m := range orphaned {
		if err := s.storageRepo.Delete(ctx, m.StorageKey); err != nil {
			s.log.Error().Err(err).Str("key", m.StorageKey).Msg("failed to delete orphaned media from storage")
		}
		if m.ThumbnailKey != "" {
			_ = s.storageRepo.Delete(ctx, m.ThumbnailKey)
		}
		if err := s.mediaRepo.Delete(ctx, m.MediaID); err != nil {
			s.log.Error().Err(err).Str("media_id", m.MediaID).Msg("failed to delete orphaned media metadata")
		}
		s.log.Info().Str("media_id", m.MediaID).Msg("deleted orphaned media")
	}

	s.log.Info().Int("count", len(orphaned)).Msg("orphaned media cleanup complete")
}
