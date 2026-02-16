CREATE TABLE IF NOT EXISTS privacy_settings (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    last_seen VARCHAR(10) NOT NULL DEFAULT 'everyone' CHECK (last_seen IN ('everyone', 'contacts', 'nobody')),
    profile_photo VARCHAR(10) NOT NULL DEFAULT 'everyone' CHECK (profile_photo IN ('everyone', 'contacts', 'nobody')),
    about VARCHAR(10) NOT NULL DEFAULT 'everyone' CHECK (about IN ('everyone', 'contacts', 'nobody')),
    read_receipts BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
