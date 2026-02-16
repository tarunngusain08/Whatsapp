package service

import "sync"

// presenceTracker tracks presence subscriptions.
// A subscription means: "subscriberUserID is interested in targetUserID's presence changes"
type presenceTracker struct {
	mu sync.RWMutex
	// userToSubscribers: targetUserID -> set of subscriber userIDs
	userToSubscribers map[string]map[string]struct{}
	// subscriberToTargets: subscriberUserID -> set of targetUserIDs they watch
	subscriberToTargets map[string]map[string]struct{}
}

func newPresenceTracker() *presenceTracker {
	return &presenceTracker{
		userToSubscribers:   make(map[string]map[string]struct{}),
		subscriberToTargets: make(map[string]map[string]struct{}),
	}
}

// Subscribe registers that subscriberID wants to watch targetIDs' presence.
func (pt *presenceTracker) Subscribe(subscriberID string, targetIDs []string) {
	pt.mu.Lock()
	defer pt.mu.Unlock()

	if _, ok := pt.subscriberToTargets[subscriberID]; !ok {
		pt.subscriberToTargets[subscriberID] = make(map[string]struct{})
	}

	for _, tid := range targetIDs {
		pt.subscriberToTargets[subscriberID][tid] = struct{}{}

		if _, ok := pt.userToSubscribers[tid]; !ok {
			pt.userToSubscribers[tid] = make(map[string]struct{})
		}
		pt.userToSubscribers[tid][subscriberID] = struct{}{}
	}
}

// GetSubscribers returns all subscriber user IDs interested in the given user's presence.
func (pt *presenceTracker) GetSubscribers(userID string) []string {
	pt.mu.RLock()
	defer pt.mu.RUnlock()

	subs := pt.userToSubscribers[userID]
	out := make([]string, 0, len(subs))
	for sid := range subs {
		out = append(out, sid)
	}
	return out
}

// Unsubscribe removes all subscriptions for a given subscriber (e.g., on disconnect).
func (pt *presenceTracker) Unsubscribe(subscriberID string) {
	pt.mu.Lock()
	defer pt.mu.Unlock()

	targets := pt.subscriberToTargets[subscriberID]
	for tid := range targets {
		if subs, ok := pt.userToSubscribers[tid]; ok {
			delete(subs, subscriberID)
			if len(subs) == 0 {
				delete(pt.userToSubscribers, tid)
			}
		}
	}
	delete(pt.subscriberToTargets, subscriberID)
}
