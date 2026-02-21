package repository

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/rs/zerolog"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"

	"github.com/whatsapp-clone/backend/message-service/internal/model"
)

type messageMongoRepo struct {
	col *mongo.Collection
	log zerolog.Logger
}

func NewMessageMongoRepository(db *mongo.Database, log zerolog.Logger) MessageRepository {
	col := db.Collection("messages")

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	indexes := []mongo.IndexModel{
		{
			Keys:    bson.D{{Key: "message_id", Value: 1}},
			Options: options.Index().SetUnique(true),
		},
		{
			Keys:    bson.D{{Key: "client_msg_id", Value: 1}},
			Options: options.Index().SetUnique(true).SetSparse(true),
		},
		{
			Keys: bson.D{
				{Key: "chat_id", Value: 1},
				{Key: "created_at", Value: -1},
				{Key: "message_id", Value: -1},
			},
		},
		{
			Keys: bson.D{{Key: "payload.body", Value: "text"}},
		},
	}

	if _, err := col.Indexes().CreateMany(ctx, indexes); err != nil {
		log.Warn().Err(err).Msg("failed to ensure indexes on messages collection")
	}

	return &messageMongoRepo{col: col, log: log}
}

// Insert creates a new message with idempotency on client_msg_id.
func (r *messageMongoRepo) Insert(ctx context.Context, msg *model.Message) (*model.Message, error) {
	_, err := r.col.InsertOne(ctx, msg)
	if err != nil {
		if mongo.IsDuplicateKeyError(err) {
			var existing model.Message
			findErr := r.col.FindOne(ctx, bson.M{"client_msg_id": msg.ClientMsgID}).Decode(&existing)
			if findErr != nil {
				return nil, fmt.Errorf("duplicate client_msg_id but failed to find existing: %w", findErr)
			}
			return &existing, nil
		}
		return nil, err
	}
	return msg, nil
}

// GetByID retrieves a single message by message_id. Returns (nil, nil) if not found.
func (r *messageMongoRepo) GetByID(ctx context.Context, messageID string) (*model.Message, error) {
	var msg model.Message
	err := r.col.FindOne(ctx, bson.M{"message_id": messageID}).Decode(&msg)
	if err != nil {
		if errors.Is(err, mongo.ErrNoDocuments) {
			return nil, nil
		}
		return nil, err
	}
	return &msg, nil
}

// ListByChatID returns messages using cursor-based pagination.
// Sorted by (created_at desc, message_id desc). Filters out deleted messages.
func (r *messageMongoRepo) ListByChatID(ctx context.Context, chatID string, cursorTime *time.Time, cursorID string, limit int) ([]*model.Message, error) {
	if limit <= 0 || limit > 100 {
		limit = 50
	}

	filter := bson.M{
		"chat_id":    chatID,
		"is_deleted": false,
	}

	if cursorTime != nil {
		filter["$or"] = bson.A{
			bson.M{"created_at": bson.M{"$lt": *cursorTime}},
			bson.M{
				"created_at": *cursorTime,
				"message_id": bson.M{"$lt": cursorID},
			},
		}
	}

	opts := options.Find().
		SetSort(bson.D{{Key: "created_at", Value: -1}, {Key: "message_id", Value: -1}}).
		SetLimit(int64(limit))

	cursor, err := r.col.Find(ctx, filter, opts)
	if err != nil {
		return nil, err
	}
	defer cursor.Close(ctx)

	var messages []*model.Message
	if err := cursor.All(ctx, &messages); err != nil {
		return nil, err
	}
	return messages, nil
}

// UpdateStatus atomically updates a recipient's status with monotonicity enforcement.
// Status transitions: sent -> delivered -> read. Backward transitions are no-ops.
func (r *messageMongoRepo) UpdateStatus(ctx context.Context, messageID, userID string, recipientStatus model.RecipientStatus) error {
	statusKey := fmt.Sprintf("status.%s", userID)
	statusStatusKey := fmt.Sprintf("status.%s.status", userID)
	statusUpdatedKey := fmt.Sprintf("status.%s.updated_at", userID)

	var allowedPrev []string
	switch recipientStatus.Status {
	case model.StatusDelivered:
		allowedPrev = []string{string(model.StatusSent)}
	case model.StatusRead:
		allowedPrev = []string{string(model.StatusSent), string(model.StatusDelivered)}
	default:
		allowedPrev = []string{}
	}

	filter := bson.M{
		"message_id": messageID,
		"$or": bson.A{
			bson.M{statusKey: bson.M{"$exists": false}},
			bson.M{statusStatusKey: bson.M{"$in": allowedPrev}},
		},
	}

	update := bson.M{
		"$set": bson.M{
			statusStatusKey:  string(recipientStatus.Status),
			statusUpdatedKey: recipientStatus.UpdatedAt,
			"updated_at":     time.Now(),
		},
	}

	result, err := r.col.UpdateOne(ctx, filter, update)
	if err != nil {
		return err
	}

	if result.MatchedCount == 0 {
		count, err := r.col.CountDocuments(ctx, bson.M{"message_id": messageID})
		if err != nil {
			return err
		}
		if count == 0 {
			return mongo.ErrNoDocuments
		}
		// Message exists but status transition not allowed — no-op.
	}

	return nil
}

// SoftDelete marks a message as deleted and clears its payload.
// Only the sender (verified by senderID) can delete.
func (r *messageMongoRepo) SoftDelete(ctx context.Context, messageID, senderID string) error {
	result, err := r.col.UpdateOne(ctx,
		bson.M{"message_id": messageID, "sender_id": senderID},
		bson.M{"$set": bson.M{
			"is_deleted": true,
			"payload":    model.MessagePayload{},
			"updated_at": time.Now(),
		}},
	)
	if err != nil {
		return err
	}
	if result.MatchedCount == 0 {
		return mongo.ErrNoDocuments
	}
	return nil
}

func (r *messageMongoRepo) SoftDeleteForUser(ctx context.Context, messageID, userID string) error {
	result, err := r.col.UpdateOne(ctx,
		bson.M{"message_id": messageID},
		bson.M{"$addToSet": bson.M{"deleted_for_users": userID}},
	)
	if err != nil {
		return err
	}
	if result.MatchedCount == 0 {
		return mongo.ErrNoDocuments
	}
	return nil
}

// StarMessage adds userID to the is_starred_by array (idempotent via $addToSet).
func (r *messageMongoRepo) StarMessage(ctx context.Context, messageID, userID string) error {
	result, err := r.col.UpdateOne(ctx,
		bson.M{"message_id": messageID},
		bson.M{"$addToSet": bson.M{"is_starred_by": userID}},
	)
	if err != nil {
		return err
	}
	if result.MatchedCount == 0 {
		return mongo.ErrNoDocuments
	}
	return nil
}

// UnstarMessage removes userID from the is_starred_by array.
func (r *messageMongoRepo) UnstarMessage(ctx context.Context, messageID, userID string) error {
	result, err := r.col.UpdateOne(ctx,
		bson.M{"message_id": messageID},
		bson.M{"$pull": bson.M{"is_starred_by": userID}},
	)
	if err != nil {
		return err
	}
	if result.MatchedCount == 0 {
		return mongo.ErrNoDocuments
	}
	return nil
}

// AddReaction adds or replaces a user's reaction on a message (one reaction per user).
// Uses an aggregation pipeline update to atomically remove any existing reaction
// and add the new one in a single operation.
func (r *messageMongoRepo) AddReaction(ctx context.Context, messageID, userID, emoji string) error {
	now := time.Now()
	pipeline := mongo.Pipeline{
		bson.D{{Key: "$set", Value: bson.M{
			"reactions": bson.M{
				"$concatArrays": bson.A{
					bson.M{"$filter": bson.M{
						"input": bson.M{"$ifNull": bson.A{"$reactions", bson.A{}}},
						"cond":  bson.M{"$ne": bson.A{"$$this.user_id", userID}},
					}},
					bson.A{bson.M{
						"emoji":      emoji,
						"user_id":    userID,
						"created_at": now,
					}},
				},
			},
			"updated_at": now,
		}}},
	}

	result, err := r.col.UpdateOne(ctx, bson.M{"message_id": messageID}, pipeline)
	if err != nil {
		return err
	}
	if result.MatchedCount == 0 {
		return mongo.ErrNoDocuments
	}
	return nil
}

// RemoveReaction removes a user's reaction from a message.
func (r *messageMongoRepo) RemoveReaction(ctx context.Context, messageID, userID string) error {
	result, err := r.col.UpdateOne(ctx,
		bson.M{"message_id": messageID},
		bson.M{"$pull": bson.M{"reactions": bson.M{"user_id": userID}}},
	)
	if err != nil {
		return err
	}
	if result.MatchedCount == 0 {
		return mongo.ErrNoDocuments
	}
	return nil
}

// Search performs a full-text search on payload.body within a chat.
func (r *messageMongoRepo) Search(ctx context.Context, chatID, query string, limit int) ([]*model.Message, error) {
	if limit <= 0 || limit > 100 {
		limit = 20
	}

	filter := bson.M{
		"chat_id":    chatID,
		"is_deleted": false,
		"$text":      bson.M{"$search": query},
	}

	opts := options.Find().
		SetSort(bson.D{{Key: "created_at", Value: -1}}).
		SetLimit(int64(limit))

	cursor, err := r.col.Find(ctx, filter, opts)
	if err != nil {
		return nil, err
	}
	defer cursor.Close(ctx)

	var messages []*model.Message
	if err := cursor.All(ctx, &messages); err != nil {
		return nil, err
	}
	return messages, nil
}

// SearchGlobal performs a full-text search across multiple chats.
func (r *messageMongoRepo) SearchGlobal(ctx context.Context, chatIDs []string, query string, limit int) ([]*model.Message, error) {
	if limit <= 0 || limit > 100 {
		limit = 20
	}

	filter := bson.M{
		"chat_id":    bson.M{"$in": chatIDs},
		"is_deleted": false,
		"$text":      bson.M{"$search": query},
	}

	opts := options.Find().
		SetSort(bson.D{{Key: "created_at", Value: -1}}).
		SetLimit(int64(limit))

	cursor, err := r.col.Find(ctx, filter, opts)
	if err != nil {
		return nil, err
	}
	defer cursor.Close(ctx)

	var messages []*model.Message
	if err := cursor.All(ctx, &messages); err != nil {
		return nil, err
	}
	return messages, nil
}

// GetLastPerChat returns the latest message for each chat using aggregation.
func (r *messageMongoRepo) GetLastPerChat(ctx context.Context, chatIDs []string) (map[string]*model.Message, error) {
	pipeline := mongo.Pipeline{
		bson.D{{Key: "$match", Value: bson.M{
			"chat_id":    bson.M{"$in": chatIDs},
			"is_deleted": false,
		}}},
		bson.D{{Key: "$sort", Value: bson.D{
			{Key: "created_at", Value: -1},
		}}},
		bson.D{{Key: "$group", Value: bson.M{
			"_id":     "$chat_id",
			"message": bson.M{"$first": "$$ROOT"},
		}}},
	}

	cursor, err := r.col.Aggregate(ctx, pipeline)
	if err != nil {
		return nil, err
	}
	defer cursor.Close(ctx)

	type aggResult struct {
		ChatID  string        `bson:"_id"`
		Message model.Message `bson:"message"`
	}

	var results []aggResult
	if err := cursor.All(ctx, &results); err != nil {
		return nil, err
	}

	lastMessages := make(map[string]*model.Message, len(results))
	for _, r := range results {
		msg := r.Message
		lastMessages[r.ChatID] = &msg
	}
	return lastMessages, nil
}

// DeleteExpiredMessages soft-deletes messages older than the given cutoff time.
// Used by the disappearing messages cleanup job.
func (r *messageMongoRepo) DeleteExpiredMessages(ctx context.Context, olderThan time.Time) (int64, error) {
	filter := bson.M{
		"created_at": bson.M{"$lt": olderThan},
		"is_deleted": false,
	}

	result, err := r.col.UpdateMany(ctx, filter, bson.M{
		"$set": bson.M{
			"is_deleted": true,
			"payload":    bson.M{},
			"updated_at": time.Now(),
		},
	})
	if err != nil {
		return 0, err
	}
	return result.ModifiedCount, nil
}

// CountUnread counts messages per chat that the given user has not read.
// Uses aggregation to batch across all requested chat IDs.
func (r *messageMongoRepo) CountUnread(ctx context.Context, userID string, chatIDs []string) (map[string]int64, error) {
	statusKey := fmt.Sprintf("status.%s", userID)
	statusStatusKey := fmt.Sprintf("status.%s.status", userID)

	pipeline := mongo.Pipeline{
		bson.D{{Key: "$match", Value: bson.M{
			"chat_id":    bson.M{"$in": chatIDs},
			"sender_id":  bson.M{"$ne": userID},
			"is_deleted": false,
			"$or": bson.A{
				bson.M{statusKey: bson.M{"$exists": false}},
				bson.M{statusStatusKey: bson.M{"$ne": string(model.StatusRead)}},
			},
		}}},
		bson.D{{Key: "$group", Value: bson.M{
			"_id":   "$chat_id",
			"count": bson.M{"$sum": 1},
		}}},
	}

	cursor, err := r.col.Aggregate(ctx, pipeline)
	if err != nil {
		return nil, err
	}
	defer cursor.Close(ctx)

	result := make(map[string]int64, len(chatIDs))
	for cursor.Next(ctx) {
		var item struct {
			ID    string `bson:"_id"`
			Count int64  `bson:"count"`
		}
		if err := cursor.Decode(&item); err != nil {
			return nil, err
		}
		result[item.ID] = item.Count
	}

	for _, chatID := range chatIDs {
		if _, ok := result[chatID]; !ok {
			result[chatID] = 0
		}
	}

	return result, nil
}
