db = db.getSiblingDB('whatsapp');

// Messages collection indexes
db.messages.createIndex({ "chat_id": 1, "created_at": -1 });
db.messages.createIndex({ "client_msg_id": 1 }, { unique: true, sparse: true });
db.messages.createIndex({ "payload.body": "text" });
db.messages.createIndex({ "sender_id": 1, "created_at": -1 });
db.messages.createIndex({ "chat_id": 1, "message_id": 1 });

// Media collection indexes
db.media.createIndex({ "media_id": 1 }, { unique: true });
db.media.createIndex({ "uploader_id": 1 });
db.media.createIndex({ "created_at": 1 });

print("MongoDB indexes created successfully");
