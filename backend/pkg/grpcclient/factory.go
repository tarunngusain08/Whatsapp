package grpcclient

import (
	"context"
	"fmt"
	"sync"
	"time"

	"github.com/sony/gobreaker/v2"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/status"
)

type Config struct {
	Address       string
	Timeout       time.Duration
	MaxRetries    int
	RetryBackoff  time.Duration
	CBMaxRequests uint32
	CBInterval    time.Duration
	CBTimeout     time.Duration
}

type Factory struct {
	mu    sync.RWMutex
	conns map[string]*grpc.ClientConn
	cbs   map[string]*gobreaker.CircuitBreaker[any]
}

func NewFactory() *Factory {
	return &Factory{
		conns: make(map[string]*grpc.ClientConn),
		cbs:   make(map[string]*gobreaker.CircuitBreaker[any]),
	}
}

func (f *Factory) GetConnection(cfg Config) (*grpc.ClientConn, error) {
	f.mu.RLock()
	if conn, ok := f.conns[cfg.Address]; ok {
		f.mu.RUnlock()
		return conn, nil
	}
	f.mu.RUnlock()

	f.mu.Lock()
	defer f.mu.Unlock()

	if conn, ok := f.conns[cfg.Address]; ok {
		return conn, nil
	}

	conn, err := grpc.NewClient(
		cfg.Address,
		grpc.WithTransportCredentials(insecure.NewCredentials()),
		grpc.WithUnaryInterceptor(f.retryInterceptor(cfg)),
	)
	if err != nil {
		return nil, fmt.Errorf("grpcclient: dial %s: %w", cfg.Address, err)
	}

	cbMaxReq := cfg.CBMaxRequests
	if cbMaxReq == 0 {
		cbMaxReq = 5
	}
	cbInterval := cfg.CBInterval
	if cbInterval == 0 {
		cbInterval = 60 * time.Second
	}
	cbTimeout := cfg.CBTimeout
	if cbTimeout == 0 {
		cbTimeout = 30 * time.Second
	}

	f.conns[cfg.Address] = conn
	f.cbs[cfg.Address] = gobreaker.NewCircuitBreaker[any](gobreaker.Settings{
		Name:        cfg.Address,
		MaxRequests: cbMaxReq,
		Interval:    cbInterval,
		Timeout:     cbTimeout,
		ReadyToTrip: func(counts gobreaker.Counts) bool {
			return counts.ConsecutiveFailures > 5
		},
	})

	return conn, nil
}

func (f *Factory) retryInterceptor(cfg Config) grpc.UnaryClientInterceptor {
	return func(
		ctx context.Context,
		method string,
		req, reply interface{},
		cc *grpc.ClientConn,
		invoker grpc.UnaryInvoker,
		opts ...grpc.CallOption,
	) error {
		cb := f.cbs[cfg.Address]
		timeout := cfg.Timeout
		if timeout == 0 {
			timeout = 5 * time.Second
		}
		maxRetries := cfg.MaxRetries
		if maxRetries == 0 {
			maxRetries = 3
		}
		backoff := cfg.RetryBackoff
		if backoff == 0 {
			backoff = 100 * time.Millisecond
		}

		var lastErr error
		for attempt := 0; attempt <= maxRetries; attempt++ {
			_, cbErr := cb.Execute(func() (any, error) {
				callCtx, cancel := context.WithTimeout(ctx, timeout)
				defer cancel()
				err := invoker(callCtx, method, req, reply, cc, opts...)
				if err != nil {
					return nil, err
				}
				return nil, nil
			})
			if cbErr == nil {
				return nil
			}
			lastErr = cbErr

			st, ok := status.FromError(cbErr)
			if !ok || !isRetryable(st.Code()) {
				return cbErr
			}

			time.Sleep(backoff * time.Duration(1<<uint(attempt)))
		}
		return lastErr
	}
}

func isRetryable(code codes.Code) bool {
	switch code {
	case codes.Unavailable, codes.DeadlineExceeded, codes.ResourceExhausted:
		return true
	default:
		return false
	}
}

func (f *Factory) Close() {
	f.mu.Lock()
	defer f.mu.Unlock()
	for addr, conn := range f.conns {
		conn.Close()
		delete(f.conns, addr)
	}
}
