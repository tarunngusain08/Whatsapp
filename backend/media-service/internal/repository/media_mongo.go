package repository

import (
	"context"
	"errors"
	"time"

	"github.com/rs/zerolog"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"

	"github.com/whatsapp-clone/backend/media-service/internal/model"
)

type mediaMongoRepo struct {
	col *mongo.Collection
	log zerolog.Logger
}

func NewMediaMongoRepository(db *mongo.Database, log zerolog.Logger) MediaRepository {
	col := db.Collection("media")

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	indexes := []mongo.IndexModel{
		{
			Keys:    bson.D{{Key: "media_id", Value: 1}},
			Options: options.Index().SetUnique(true),
		},
		{
			Keys: bson.D{{Key: "created_at", Value: 1}},
		},
		{
			Keys: bson.D{{Key: "uploader_id", Value: 1}},
		},
	}

	if _, err := col.Indexes().CreateMany(ctx, indexes); err != nil {
		log.Warn().Err(err).Msg("failed to ensure indexes on media collection")
	}

	return &mediaMongoRepo{col: col, log: log}
}

func (r *mediaMongoRepo) Insert(ctx context.Context, media *model.Media) error {
	_, err := r.col.InsertOne(ctx, media)
	if err != nil {
		if mongo.IsDuplicateKeyError(err) {
			return errors.New("media already exists")
		}
		return err
	}
	return nil
}

func (r *mediaMongoRepo) GetByID(ctx context.Context, mediaID string) (*model.Media, error) {
	var media model.Media
	err := r.col.FindOne(ctx, bson.M{"media_id": mediaID}).Decode(&media)
	if err != nil {
		if errors.Is(err, mongo.ErrNoDocuments) {
			return nil, nil
		}
		return nil, err
	}
	return &media, nil
}

func (r *mediaMongoRepo) Delete(ctx context.Context, mediaID string) error {
	result, err := r.col.DeleteOne(ctx, bson.M{"media_id": mediaID})
	if err != nil {
		return err
	}
	if result.DeletedCount == 0 {
		return mongo.ErrNoDocuments
	}
	return nil
}

// FindOrphaned returns media older than the given time that are considered orphaned.
// In a full implementation this would cross-reference the messages collection;
// here we simplify to media older than the cutoff as a conservative approach.
func (r *mediaMongoRepo) FindOrphaned(ctx context.Context, olderThan time.Time) ([]*model.Media, error) {
	filter := bson.M{
		"created_at": bson.M{"$lt": olderThan},
	}

	cursor, err := r.col.Find(ctx, filter, options.Find().SetLimit(500))
	if err != nil {
		return nil, err
	}
	defer cursor.Close(ctx)

	var media []*model.Media
	if err := cursor.All(ctx, &media); err != nil {
		return nil, err
	}
	return media, nil
}
