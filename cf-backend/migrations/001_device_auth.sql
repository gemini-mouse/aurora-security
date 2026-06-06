CREATE TABLE IF NOT EXISTS device_auth (
  userId TEXT PRIMARY KEY,
  tokenHash TEXT NOT NULL UNIQUE,
  installSecretHash TEXT,
  deviceName TEXT,
  createdAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updatedAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lastIssuedAt TEXT,
  lastUsedAt TEXT,
  revokedAt TEXT,
  FOREIGN KEY (userId) REFERENCES users(userId)
);

CREATE INDEX IF NOT EXISTS idx_device_auth_token_hash
ON device_auth(tokenHash);

CREATE TRIGGER IF NOT EXISTS device_auth_set_updated_at
AFTER UPDATE ON device_auth
FOR EACH ROW
WHEN NEW.updatedAt = OLD.updatedAt AND NEW.updatedAt != CURRENT_TIMESTAMP
BEGIN
  UPDATE device_auth
  SET updatedAt = CURRENT_TIMESTAMP
  WHERE userId = NEW.userId;
END;
