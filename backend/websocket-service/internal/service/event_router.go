package service

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	messagev1 "github.com/whatsapp-clone/backend/proto/message/v1"
	"github.com/whatsapp-clone/backend/websocket-service/internal/model"
)

// HandleEvent routes a client->server event to the appropriate handler.
func (s *wsServiceImpl) HandleEvent(ctx context.Context, client *model.Client, event *model.WSEvent) error {
	switch event.Type {
	case "message.send":
		return s.handleMessageSend(ctx, client, event.Payload)
	case "message.delivered":
		return s.handleMessageStatus(ctx, client, event.Payload, "delivered")
	case "message.read":
		return s.handleMessageStatus(ctx, client, event.Payload, "read")
	case "message.delete":
		return s.handleMessageDelete(ctx, client, event.Payload)
	case "typing.start":
		return s.handleTyping(ctx, client, event.Payload, true)
	case "typing.stop":
		return s.handleTyping(ctx, client, event.Payload, false)
	case "presence.subscribe":
		return s.handlePresenceSubscribe(ctx, client, event.Payload)
	case "call.offer":
		return s.handleCallOffer(ctx, client, event.Payload)
	case "call.answer":
		return s.handleCallAnswer(ctx, client, event.Payload)
	case "call.ice-candidate":
		return s.handleCallIceCandidate(ctx, client, event.Payload)
	case "call.end":
		return s.handleCallEnd(ctx, client, event.Payload)
	case "ping":
		return s.handlePing(client)
	default:
		return fmt.Errorf("unknown event type: %s", event.Type)
	}
}

func (s *wsServiceImpl) handleMessageSend(ctx context.Context, client *model.Client, payload json.RawMessage) error {
	var p model.MessageSendPayload
	if err := json.Unmarshal(payload, &p); err != nil {
		return fmt.Errorf("invalid message.send payload: %w", err)
	}

	resp, err := s.messageClient.SendMessage(ctx, &messagev1.SendMessageRequest{
		ChatId:           p.ChatID,
		SenderId:         client.UserID,
		Type:             p.Type,
		ClientMsgId:      p.ClientMsgID,
		ReplyToMessageId: p.ReplyToMessageID,
		Payload: &messagev1.MessagePayload{
			Body:       p.Payload.Body,
			MediaId:    p.Payload.MediaID,
			Caption:    p.Payload.Caption,
			Filename:   p.Payload.Filename,
			DurationMs: p.Payload.DurationMs,
		},
	})
	if err != nil {
		return fmt.Errorf("message-service SendMessage failed: %w", err)
	}

	ack := model.WSEvent{Type: "message.sent"}
	ack.Payload, _ = json.Marshal(model.MessageSentAckPayload{
		ClientMsgID: p.ClientMsgID,
		MessageID:   resp.MessageId,
		CreatedAt:   resp.CreatedAt.AsTime().UnixMilli(),
	})
	return s.SendToUser(client.UserID, &ack)
}

func (s *wsServiceImpl) handleMessageStatus(ctx context.Context, client *model.Client, payload json.RawMessage, statusVal string) error {
	var p model.MessageStatusPayload
	if err := json.Unmarshal(payload, &p); err != nil {
		return fmt.Errorf("invalid status payload: %w", err)
	}

	// Fast path: push the status update directly to the original message
	// sender's WebSocket connection to avoid the gRPC -> NATS -> Redis
	// round-trip. If sender_id is provided, target only that user;
	// otherwise fall back to all other participants (for backwards compat).
	if p.ChatID != "" {
		statusEvent := model.WSEvent{Type: "message.status"}
		statusEvent.Payload, _ = json.Marshal(map[string]string{
			"message_id": p.MessageID,
			"chat_id":    p.ChatID,
			"user_id":    client.UserID,
			"status":     statusVal,
		})
		data, _ := json.Marshal(statusEvent)

		if p.SenderID != "" && p.SenderID != client.UserID {
			s.rdb.Publish(ctx, "user:channel:"+p.SenderID, data)
		} else {
			participants := s.getChatParticipants(ctx, p.ChatID)
			for _, uid := range participants {
				if uid == client.UserID {
					continue
				}
				s.rdb.Publish(ctx, "user:channel:"+uid, data)
			}
		}
	}

	// Async persistence via gRPC to message-service (runs in background)
	go func() {
		bgCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		_, err := s.messageClient.UpdateMessageStatus(bgCtx, &messagev1.UpdateMessageStatusRequest{
			MessageId: p.MessageID,
			UserId:    client.UserID,
			Status:    statusVal,
		})
		if err != nil {
			s.log.Error().Err(err).
				Str("message_id", p.MessageID).
				Str("status", statusVal).
				Msg("async status persist failed")
		}
	}()

	return nil
}

func (s *wsServiceImpl) handleMessageDelete(ctx context.Context, client *model.Client, payload json.RawMessage) error {
	var p model.MessageDeletePayload
	if err := json.Unmarshal(payload, &p); err != nil {
		return fmt.Errorf("invalid message.delete payload: %w", err)
	}

	data, _ := json.Marshal(map[string]interface{}{
		"message_id":   p.MessageID,
		"chat_id":      p.ChatID,
		"user_id":      client.UserID,
		"for_everyone": p.ForEveryone,
	})
	_, err := s.js.Publish("msg.deleted", data)
	if err != nil {
		return fmt.Errorf("publish msg.deleted: %w", err)
	}
	return nil
}

func (s *wsServiceImpl) handleTyping(ctx context.Context, client *model.Client, payload json.RawMessage, start bool) error {
	var p model.TypingPayload
	if err := json.Unmarshal(payload, &p); err != nil {
		return fmt.Errorf("invalid typing payload: %w", err)
	}

	key := fmt.Sprintf("typing:%s:%s", p.ChatID, client.UserID)
	if start {
		s.rdb.SetEx(ctx, key, "1", s.cfg.TypingTTL)
	} else {
		s.rdb.Del(ctx, key)
	}

	event := model.WSEvent{Type: "typing"}
	event.Payload, _ = json.Marshal(map[string]interface{}{
		"chat_id": p.ChatID,
		"user_id": client.UserID,
		"typing":  start,
	})
	data, _ := json.Marshal(event)

	participants := s.getChatParticipants(ctx, p.ChatID)
	for _, uid := range participants {
		if uid == client.UserID {
			continue
		}
		s.rdb.Publish(ctx, "user:channel:"+uid, data)
	}

	return nil
}

func (s *wsServiceImpl) handlePresenceSubscribe(ctx context.Context, client *model.Client, payload json.RawMessage) error {
	var p model.PresenceSubscribePayload
	if err := json.Unmarshal(payload, &p); err != nil {
		return err
	}

	// Track the subscription for future presence change notifications
	s.presenceTracker.Subscribe(client.UserID, p.UserIDs)

	// Return current presence state for each requested user
	for _, uid := range p.UserIDs {
		online := s.hub.IsConnected(uid)
		event := model.WSEvent{Type: "presence"}
		event.Payload, _ = json.Marshal(model.PresenceEventPayload{
			UserID: uid,
			Online: online,
		})
		s.SendToUser(client.UserID, &event)
	}
	return nil
}

func (s *wsServiceImpl) handleCallOffer(ctx context.Context, client *model.Client, payload json.RawMessage) error {
	var p model.CallOfferPayload
	if err := json.Unmarshal(payload, &p); err != nil {
		return fmt.Errorf("invalid call.offer payload: %w", err)
	}

	event := model.WSEvent{Type: "call.offer"}
	event.Payload, _ = json.Marshal(map[string]string{
		"call_id":   p.CallID,
		"caller_id": client.UserID,
		"sdp":       p.SDP,
		"call_type": p.CallType,
	})
	data, _ := json.Marshal(event)
	s.rdb.Publish(ctx, "user:channel:"+p.TargetUserID, data)
	return nil
}

func (s *wsServiceImpl) handleCallAnswer(ctx context.Context, client *model.Client, payload json.RawMessage) error {
	var p model.CallAnswerPayload
	if err := json.Unmarshal(payload, &p); err != nil {
		return fmt.Errorf("invalid call.answer payload: %w", err)
	}

	event := model.WSEvent{Type: "call.answer"}
	event.Payload, _ = json.Marshal(map[string]string{
		"call_id":     p.CallID,
		"answerer_id": client.UserID,
		"sdp":         p.SDP,
	})
	data, _ := json.Marshal(event)
	s.rdb.Publish(ctx, "user:channel:"+p.TargetUserID, data)
	return nil
}

func (s *wsServiceImpl) handleCallIceCandidate(ctx context.Context, client *model.Client, payload json.RawMessage) error {
	var p model.CallIceCandidatePayload
	if err := json.Unmarshal(payload, &p); err != nil {
		return fmt.Errorf("invalid call.ice-candidate payload: %w", err)
	}

	event := model.WSEvent{Type: "call.ice-candidate"}
	event.Payload, _ = json.Marshal(map[string]string{
		"call_id":   p.CallID,
		"sender_id": client.UserID,
		"candidate": p.Candidate,
	})
	data, _ := json.Marshal(event)
	s.rdb.Publish(ctx, "user:channel:"+p.TargetUserID, data)
	return nil
}

func (s *wsServiceImpl) handleCallEnd(ctx context.Context, client *model.Client, payload json.RawMessage) error {
	var p model.CallEndPayload
	if err := json.Unmarshal(payload, &p); err != nil {
		return fmt.Errorf("invalid call.end payload: %w", err)
	}

	event := model.WSEvent{Type: "call.end"}
	event.Payload, _ = json.Marshal(map[string]string{
		"call_id":   p.CallID,
		"sender_id": client.UserID,
		"reason":    p.Reason,
	})
	data, _ := json.Marshal(event)
	s.rdb.Publish(ctx, "user:channel:"+p.TargetUserID, data)
	return nil
}

func (s *wsServiceImpl) handlePing(client *model.Client) error {
	_ = s.SetPresence(context.Background(), client.UserID, true)

	pong := model.WSEvent{Type: "pong"}
	pong.Payload, _ = json.Marshal(map[string]int64{"timestamp": time.Now().UnixMilli()})
	return s.SendToUser(client.UserID, &pong)
}
