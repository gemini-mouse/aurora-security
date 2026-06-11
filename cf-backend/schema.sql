CREATE TABLE IF NOT EXISTS users (
  userId TEXT PRIMARY KEY,
  bindCode TEXT UNIQUE,
  deviceName TEXT,
  createdAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updatedAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lastRegisteredAt TEXT,
  lastAlertSentAt TEXT
);

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

CREATE TABLE IF NOT EXISTS telegram_contacts (
  id TEXT,
  userId TEXT,
  name TEXT,
  telegramHandle TEXT,
  status TEXT,
  chatId INTEGER,
  createdAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updatedAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  boundAt TEXT,
  lastAlertSentAt TEXT,
  PRIMARY KEY (id, userId),
  FOREIGN KEY (userId) REFERENCES users(userId)
);

CREATE TABLE IF NOT EXISTS push_devices (
  userId TEXT PRIMARY KEY,
  name TEXT,
  deviceName TEXT,
  fcmToken TEXT NOT NULL,
  createdAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updatedAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lastRegisteredAt TEXT
);

CREATE TABLE IF NOT EXISTS push_contacts (
  userId TEXT,
  contactUserId TEXT,
  status TEXT,
  createdAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updatedAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  boundAt TEXT,
  lastAlertSentAt TEXT,
  PRIMARY KEY (userId, contactUserId),
  FOREIGN KEY (userId) REFERENCES users(userId),
  FOREIGN KEY (contactUserId) REFERENCES push_devices(userId)
);

CREATE TABLE IF NOT EXISTS line_contacts (
  lineUserId TEXT,
  userId TEXT,
  displayName TEXT,
  status TEXT,
  createdAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updatedAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  boundAt TEXT,
  lastAlertSentAt TEXT,
  PRIMARY KEY (lineUserId, userId),
  FOREIGN KEY (userId) REFERENCES users(userId)
);

CREATE TABLE IF NOT EXISTS training_audio (
  id TEXT PRIMARY KEY,
  hashedUserId TEXT NOT NULL,
  objectKey TEXT NOT NULL UNIQUE,
  filename TEXT NOT NULL,
  triggerSource TEXT NOT NULL,
  modelDangerLevel TEXT,
  capturedAtEpochMs INTEGER NOT NULL DEFAULT 0,
  sampleRateHz INTEGER NOT NULL DEFAULT 16000,
  durationMs INTEGER NOT NULL DEFAULT 5000,
  contentType TEXT NOT NULL DEFAULT 'audio/wav',
  byteSize INTEGER NOT NULL DEFAULT 0,
  createdAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_training_audio_hashed_user_id
ON training_audio(hashedUserId, createdAt DESC);

CREATE INDEX IF NOT EXISTS idx_device_auth_token_hash
ON device_auth(tokenHash);

CREATE TRIGGER IF NOT EXISTS users_set_updated_at
AFTER UPDATE ON users
FOR EACH ROW
WHEN NEW.updatedAt = OLD.updatedAt AND NEW.updatedAt != CURRENT_TIMESTAMP
BEGIN
  UPDATE users
  SET updatedAt = CURRENT_TIMESTAMP
  WHERE userId = NEW.userId;
END;

CREATE TRIGGER IF NOT EXISTS device_auth_set_updated_at
AFTER UPDATE ON device_auth
FOR EACH ROW
WHEN NEW.updatedAt = OLD.updatedAt AND NEW.updatedAt != CURRENT_TIMESTAMP
BEGIN
  UPDATE device_auth
  SET updatedAt = CURRENT_TIMESTAMP
  WHERE userId = NEW.userId;
END;

CREATE TRIGGER IF NOT EXISTS telegram_contacts_set_updated_at
AFTER UPDATE ON telegram_contacts
FOR EACH ROW
WHEN NEW.updatedAt = OLD.updatedAt AND NEW.updatedAt != CURRENT_TIMESTAMP
BEGIN
  UPDATE telegram_contacts
  SET updatedAt = CURRENT_TIMESTAMP
  WHERE id = NEW.id AND userId = NEW.userId;
END;

CREATE TRIGGER IF NOT EXISTS push_devices_set_updated_at
AFTER UPDATE ON push_devices
FOR EACH ROW
WHEN NEW.updatedAt = OLD.updatedAt AND NEW.updatedAt != CURRENT_TIMESTAMP
BEGIN
  UPDATE push_devices
  SET updatedAt = CURRENT_TIMESTAMP
  WHERE userId = NEW.userId;
END;

CREATE TRIGGER IF NOT EXISTS push_contacts_set_updated_at
AFTER UPDATE ON push_contacts
FOR EACH ROW
WHEN NEW.updatedAt = OLD.updatedAt AND NEW.updatedAt != CURRENT_TIMESTAMP
BEGIN
  UPDATE push_contacts
  SET updatedAt = CURRENT_TIMESTAMP
  WHERE userId = NEW.userId AND contactUserId = NEW.contactUserId;
END;

CREATE TRIGGER IF NOT EXISTS line_contacts_set_updated_at
AFTER UPDATE ON line_contacts
FOR EACH ROW
WHEN NEW.updatedAt = OLD.updatedAt AND NEW.updatedAt != CURRENT_TIMESTAMP
BEGIN
  UPDATE line_contacts
  SET updatedAt = CURRENT_TIMESTAMP
  WHERE lineUserId = NEW.lineUserId AND userId = NEW.userId;
END;
