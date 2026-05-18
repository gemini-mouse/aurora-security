import { Hono } from 'hono';

type Bindings = {
  DB: D1Database;
  TRAINING_AUDIO_BUCKET: R2Bucket;
  ALARM_API_TOKEN: string;
  TELEGRAM_BOT_TOKEN: string;
  FCM_PROJECT_ID?: string;
  FCM_CLIENT_EMAIL?: string;
  FCM_PRIVATE_KEY?: string;
  USER_ID_HASH_SALT?: string;
};

type PushDeviceRow = {
  contactUserId: string;
  name: string;
  deviceName: string;
  fcmToken: string;
};

type TrainingAudioMetadata = {
  triggerSource?: string;
  dangerLevel?: string | null;
  capturedAtEpochMs?: number;
  sampleRateHz?: number;
  durationMs?: number;
};

let cachedGoogleAccessToken: { token: string; expiresAt: number } | null = null;

const app = new Hono<{ Bindings: Bindings }>();

const requireAuth = async (c: any, next: any) => {
  const sharedToken = (c.env.ALARM_API_TOKEN || '').trim();
  if (!sharedToken) return await next();

  const authHeader = (c.req.header('Authorization') || '').trim();
  if (authHeader !== `Bearer ${sharedToken}`) {
    return c.json({ error: 'Unauthorized' }, 401);
  }
  await next();
};

async function sendTelegramMessage(botToken: string, chatId: number, text: string) {
  if (!botToken) throw new Error('Missing TELEGRAM_BOT_TOKEN');

  const payload = { chat_id: chatId, text };
  const response = await fetch(`https://api.telegram.org/bot${botToken}/sendMessage`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`Telegram sendMessage failed: ${response.status} ${body}`);
  }
}

function splitTelegramText(text: string, maxLength = 4000) {
  if (!text) return [''];
  const chunks: string[] = [];
  let remaining = text.trim();

  while (remaining.length > maxLength) {
    let splitIndex = remaining.lastIndexOf('\n', maxLength);
    if (splitIndex < maxLength * 0.5) {
      splitIndex = remaining.lastIndexOf(' ', maxLength);
    }
    if (splitIndex <= 0) {
      splitIndex = maxLength;
    }
    chunks.push(remaining.slice(0, splitIndex).trim());
    remaining = remaining.slice(splitIndex).trim();
  }

  if (remaining.length > 0) {
    chunks.push(remaining);
  } else if (chunks.length === 0) {
    chunks.push('');
  }

  return chunks;
}

async function sendTelegramLongMessage(botToken: string, chatId: number, text: string) {
  for (const chunk of splitTelegramText(text)) {
    await sendTelegramMessage(botToken, chatId, chunk);
  }
}

async function sendTelegramAudio(
  botToken: string,
  chatId: number,
  audioBase64: string,
  filename: string,
  caption: string,
) {
  if (!botToken) throw new Error('Missing TELEGRAM_BOT_TOKEN');

  const byteArray = decodeBase64ToUint8Array(audioBase64);
  const blob = new Blob([byteArray], { type: 'audio/mp4' });

  const formData = new FormData();
  formData.append('chat_id', String(chatId));
  formData.append('caption', caption || '');
  formData.append('audio', blob, filename || 'crisis-audio.m4a');

  const response = await fetch(`https://api.telegram.org/bot${botToken}/sendAudio`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`Telegram sendAudio failed: ${response.status} ${body}`);
  }
}

function decodeBase64ToUint8Array(audioBase64: string) {
  const byteCharacters = atob(audioBase64);
  const byteNumbers = new Array(byteCharacters.length);
  for (let i = 0; i < byteCharacters.length; i++) {
    byteNumbers[i] = byteCharacters.charCodeAt(i);
  }
  return new Uint8Array(byteNumbers);
}

async function sha256Hex(value: string) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('');
}

async function buildHashedUserId(env: Bindings, userId: string) {
  const salt = (env.USER_ID_HASH_SALT || '').trim();
  if (!salt) {
    return await sha256Hex(userId);
  }
  return await sha256Hex(`${salt}:${userId}`);
}

function sanitizeTelegramHandle(handle: string) {
  if (!handle) return '';
  return handle.startsWith('@') ? handle : `@${handle}`;
}

async function markUserAlertSent(db: D1Database, userId: string) {
  await db.prepare(
    'UPDATE users SET lastAlertSentAt = CURRENT_TIMESTAMP WHERE userId = ?'
  ).bind(userId).run();
}

async function markTelegramContactAlertSent(db: D1Database, userId: string, contactId: string) {
  await db.prepare(
    'UPDATE contacts SET lastAlertSentAt = CURRENT_TIMESTAMP WHERE userId = ? AND id = ?'
  ).bind(userId, contactId).run();
}

async function markPushContactAlertSent(db: D1Database, userId: string, contactUserId: string) {
  await db.prepare(
    'UPDATE push_contacts SET lastAlertSentAt = CURRENT_TIMESTAMP WHERE userId = ? AND contactUserId = ?'
  ).bind(userId, contactUserId).run();
}

function base64UrlEncodeJson(value: unknown) {
  return base64UrlEncodeText(JSON.stringify(value));
}

function base64UrlEncodeText(value: string) {
  const bytes = new TextEncoder().encode(value);
  return base64UrlEncodeBytes(bytes);
}

function base64UrlEncodeBytes(bytes: Uint8Array) {
  let binary = '';
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
}

async function importGooglePrivateKey(privateKeyPem: string) {
  const normalizedPem = privateKeyPem
    .replace(/\\n/g, '\n')
    .replace('-----BEGIN PRIVATE KEY-----', '')
    .replace('-----END PRIVATE KEY-----', '')
    .replace(/\s+/g, '');

  const binary = atob(normalizedPem);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }

  return crypto.subtle.importKey(
    'pkcs8',
    bytes.buffer,
    {
      name: 'RSASSA-PKCS1-v1_5',
      hash: 'SHA-256',
    },
    false,
    ['sign'],
  );
}

async function getGoogleAccessToken(env: Bindings) {
  if (!env.FCM_PROJECT_ID || !env.FCM_CLIENT_EMAIL || !env.FCM_PRIVATE_KEY) {
    throw new Error('Missing FCM service account configuration');
  }

  if (cachedGoogleAccessToken && cachedGoogleAccessToken.expiresAt > Date.now() + 60_000) {
    return cachedGoogleAccessToken.token;
  }

  const nowSeconds = Math.floor(Date.now() / 1000);
  const unsignedToken = [
    base64UrlEncodeJson({ alg: 'RS256', typ: 'JWT' }),
    base64UrlEncodeJson({
      iss: env.FCM_CLIENT_EMAIL,
      scope: 'https://www.googleapis.com/auth/firebase.messaging',
      aud: 'https://oauth2.googleapis.com/token',
      iat: nowSeconds,
      exp: nowSeconds + 3600,
    }),
  ].join('.');

  const privateKey = await importGooglePrivateKey(env.FCM_PRIVATE_KEY);
  const signature = await crypto.subtle.sign(
    'RSASSA-PKCS1-v1_5',
    privateKey,
    new TextEncoder().encode(unsignedToken),
  );
  const assertion = `${unsignedToken}.${base64UrlEncodeBytes(new Uint8Array(signature))}`;

  const response = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion,
    }),
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`Unable to get Google access token: ${response.status} ${body}`);
  }

  const tokenResponse = await response.json<{
    access_token: string;
    expires_in: number;
  }>();

  cachedGoogleAccessToken = {
    token: tokenResponse.access_token,
    expiresAt: Date.now() + tokenResponse.expires_in * 1000,
  };
  return tokenResponse.access_token;
}

async function sendPushNotification(
  env: Bindings,
  pushToken: string,
  title: string,
  message: string,
  ownerUserId: string,
  eventId: string,
  messageType: string,
) {
  if (!env.FCM_PROJECT_ID) {
    throw new Error('Missing FCM_PROJECT_ID');
  }

  const accessToken = await getGoogleAccessToken(env);
  const response = await fetch(
    `https://fcm.googleapis.com/v1/projects/${env.FCM_PROJECT_ID}/messages:send`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify({
        message: {
          token: pushToken,
          data: {
            ownerUserId,
            eventId,
            messageType,
            title,
            message,
            channel: 'aurora_push',
          },
          android: {
            priority: 'HIGH',
          },
        },
      }),
    },
  );

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`FCM send failed: ${response.status} ${body}`);
  }
}

app.get('/', (c) => {
  return c.text('Alarm Telegram Backend is running on Cloudflare Workers.');
});

app.get('/health', (c) => {
  return c.json({ ok: true });
});

app.get('/setup-webhook', async (c) => {
  const botToken = c.env.TELEGRAM_BOT_TOKEN;
  if (!botToken) {
    return c.text('Error: TELEGRAM_BOT_TOKEN secret is not set.', 500);
  }
  const url = new URL(c.req.url);
  const webhookUrl = `${url.protocol}//${url.host}/telegram/webhook`;
  const response = await fetch(
    `https://api.telegram.org/bot${botToken}/setWebhook?url=${encodeURIComponent(webhookUrl)}`
  );
  const result = await response.json();
  return c.json({ webhookUrl, result });
});

app.post('/users/register', requireAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { userId, bindCode, deviceName, contactLabel } = body;

  if (!userId || !bindCode) {
    return c.json({ error: 'userId and bindCode are required' }, 400);
  }

  try {
    await c.env.DB.prepare(
      `INSERT INTO users (userId, bindCode, deviceName, lastRegisteredAt)
       VALUES (?, ?, ?, CURRENT_TIMESTAMP)
       ON CONFLICT(userId) DO UPDATE SET
         bindCode = excluded.bindCode,
         deviceName = excluded.deviceName,
         lastRegisteredAt = CURRENT_TIMESTAMP`
    ).bind(userId, bindCode, deviceName || contactLabel || 'Phone').run();

    const { results: contacts } = await c.env.DB.prepare(
      'SELECT id, name, telegramHandle, status FROM contacts WHERE userId = ?'
    ).bind(userId).all();

    return c.json({
      ok: true,
      userId,
      bindCode,
      contacts: contacts || [],
    });
  } catch (err: any) {
    console.error('Register error:', err);
    return c.json({ error: 'Internal server error' }, 500);
  }
});

app.post('/push/register-device', requireAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { userId, name, deviceName, fcmToken } = body;

  if (!userId || !fcmToken) {
    return c.json({ error: 'userId and fcmToken are required' }, 400);
  }

  try {
    await c.env.DB.prepare(
      `INSERT INTO push_devices (userId, name, deviceName, fcmToken, lastRegisteredAt)
       VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
       ON CONFLICT(userId) DO UPDATE SET
         name = excluded.name,
         deviceName = excluded.deviceName,
         fcmToken = excluded.fcmToken,
         lastRegisteredAt = CURRENT_TIMESTAMP`
    ).bind(userId, name || 'Aurora contact', deviceName || 'Phone', fcmToken).run();

    return c.json({ ok: true });
  } catch (err) {
    console.error('Push register-device error:', err);
    return c.json({ error: 'Internal server error' }, 500);
  }
});

app.post('/push/bind', requireAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { bindCode, contactUserId } = body;

  if (!bindCode || !contactUserId) {
    return c.json({ error: 'bindCode and contactUserId are required' }, 400);
  }

  try {
    const owner = await c.env.DB.prepare(
      'SELECT userId FROM users WHERE bindCode = ?'
    ).bind(bindCode).first<{ userId: string }>();

    if (!owner) {
      return c.json({ error: 'Bind code not found' }, 404);
    }

    if (owner.userId === contactUserId) {
      return c.json({ error: 'A device cannot bind to itself' }, 400);
    }

    const registeredDevice = await c.env.DB.prepare(
      'SELECT userId FROM push_devices WHERE userId = ?'
    ).bind(contactUserId).first<{ userId: string }>();

    if (!registeredDevice) {
      return c.json({ error: 'Push device is not registered yet' }, 400);
    }

    await c.env.DB.prepare(
      `INSERT INTO push_contacts (userId, contactUserId, status, boundAt)
       VALUES (?, ?, 'Bound', CURRENT_TIMESTAMP)
       ON CONFLICT(userId, contactUserId) DO UPDATE SET
         status = 'Bound'`
    ).bind(owner.userId, contactUserId).run();

    return c.json({ ok: true, userId: owner.userId });
  } catch (err) {
    console.error('Push bind error:', err);
    return c.json({ error: 'Internal server error' }, 500);
  }
});

app.get('/users/:userId/contacts', requireAuth, async (c) => {
  try {
    const userId = c.req.param('userId');
    const { results: contacts } = await c.env.DB.prepare(
      'SELECT id, name, telegramHandle, status FROM contacts WHERE userId = ?'
    ).bind(userId).all();

    return c.json({ contacts: contacts || [] });
  } catch (err) {
    return c.json({ error: 'Internal server error' }, 500);
  }
});

app.get('/users/:userId/push-contacts', requireAuth, async (c) => {
  try {
    const userId = c.req.param('userId');
    const { results: contacts } = await c.env.DB.prepare(
      `SELECT
         pc.contactUserId AS id,
         COALESCE(pd.name, 'Aurora contact') AS name,
         COALESCE(pd.deviceName, 'Phone') AS deviceName,
         pc.status AS status
       FROM push_contacts pc
       LEFT JOIN push_devices pd ON pd.userId = pc.contactUserId
       WHERE pc.userId = ?
       ORDER BY pc.boundAt DESC, pc.createdAt DESC`
    ).bind(userId).all();

    return c.json({ contacts: contacts || [] });
  } catch (err) {
    console.error('Fetch push contacts error:', err);
    return c.json({ error: 'Internal server error' }, 500);
  }
});

app.delete('/users/:userId/contacts/:contactId', requireAuth, async (c) => {
  try {
    const userId = c.req.param('userId');
    const contactId = c.req.param('contactId');

    if (!userId || !contactId) {
      return c.json({ error: 'userId and contactId are required' }, 400);
    }

    const result = await c.env.DB.prepare(
      'DELETE FROM contacts WHERE userId = ? AND id = ?'
    ).bind(userId, contactId).run();

    return c.json({ ok: true, deleted: result.meta.changes || 0 });
  } catch (err) {
    console.error('Delete contact error:', err);
    return c.json({ error: 'Internal server error' }, 500);
  }
});

app.delete('/users/:userId/push-contacts/:contactUserId', requireAuth, async (c) => {
  try {
    const userId = c.req.param('userId');
    const contactUserId = c.req.param('contactUserId');

    if (!userId || !contactUserId) {
      return c.json({ error: 'userId and contactUserId are required' }, 400);
    }

    const result = await c.env.DB.prepare(
      'DELETE FROM push_contacts WHERE userId = ? AND contactUserId = ?'
    ).bind(userId, contactUserId).run();

    return c.json({ ok: true, deleted: result.meta.changes || 0 });
  } catch (err) {
    console.error('Delete push contact error:', err);
    return c.json({ error: 'Internal server error' }, 500);
  }
});

app.post('/alerts', requireAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { userId, message } = body;

  if (!userId || !message) {
    return c.json({ error: 'userId and message are required' }, 400);
  }

  try {
    const { results: boundContacts } = await c.env.DB.prepare(
      "SELECT id, chatId FROM contacts WHERE userId = ? AND status = 'Bound'"
    ).bind(userId).all();

    if (!boundContacts || boundContacts.length === 0) {
      return c.json({ ok: true, sent: 0, skipped: true });
    }

    for (const contact of boundContacts as { id: string; chatId: number }[]) {
      await sendTelegramLongMessage(c.env.TELEGRAM_BOT_TOKEN, contact.chatId, message);
      await markTelegramContactAlertSent(c.env.DB, userId, contact.id);
    }
    await markUserAlertSent(c.env.DB, userId);

    return c.json({ ok: true, sent: boundContacts.length });
  } catch (error: any) {
    console.error('[/alerts] Telegram delivery failed:', error);
    return c.json({ error: error.message }, 502);
  }
});

app.post('/alerts/audio', requireAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { userId, audioBase64, filename, caption } = body;

  if (!userId || !audioBase64) {
    return c.json({ error: 'userId and audioBase64 are required' }, 400);
  }

  try {
    const { results: boundContacts } = await c.env.DB.prepare(
      "SELECT id, chatId FROM contacts WHERE userId = ? AND status = 'Bound'"
    ).bind(userId).all();

    if (!boundContacts || boundContacts.length === 0) {
      return c.json({ ok: true, sent: 0, skipped: true });
    }

    for (const contact of boundContacts as { id: string; chatId: number }[]) {
      await sendTelegramAudio(
        c.env.TELEGRAM_BOT_TOKEN,
        contact.chatId,
        audioBase64,
        filename || 'crisis-audio.m4a',
        caption || ''
      );
      await markTelegramContactAlertSent(c.env.DB, userId, contact.id);
    }
    await markUserAlertSent(c.env.DB, userId);
    return c.json({ ok: true, sent: boundContacts.length });
  } catch (error: any) {
    console.error('[/alerts/audio] Telegram audio delivery failed:', error);
    return c.json({ error: error.message }, 502);
  }
});

app.post('/training/audio', requireAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const userId = typeof body.userId === 'string' ? body.userId.trim() : '';
  const audioBase64 = typeof body.audioBase64 === 'string' ? body.audioBase64.trim() : '';
  const filename = typeof body.filename === 'string' ? body.filename.trim() : '';
  const metadata = (body.metadata || {}) as TrainingAudioMetadata;

  if (!userId || !audioBase64) {
    return c.json({ error: 'userId and audioBase64 are required' }, 400);
  }

  try {
    const audioBytes = decodeBase64ToUint8Array(audioBase64);
    const contentType = 'audio/wav';
    const hashedUserId = await buildHashedUserId(c.env, userId);
    const normalizedFilename = filename || 'crisis-training.wav';
    const stableUploadSeed = [
      hashedUserId,
      normalizedFilename,
      String(metadata.capturedAtEpochMs ?? 0),
      String(metadata.sampleRateHz ?? 16_000),
      String(metadata.durationMs ?? 5_000),
    ].join(':');
    const uploadId = (await sha256Hex(stableUploadSeed)).slice(0, 32);
    const objectKey = `training-audio/${new Date().toISOString().slice(0, 10)}/${uploadId}.wav`;

    await c.env.TRAINING_AUDIO_BUCKET.put(objectKey, audioBytes, {
      httpMetadata: {
        contentType,
      },
      customMetadata: {
        triggerSource: metadata.triggerSource || 'unknown',
        dangerLevel: metadata.dangerLevel?.trim() || '',
        capturedAtEpochMs: String(metadata.capturedAtEpochMs ?? 0),
      },
    });

    await c.env.DB.prepare(
      `INSERT INTO audio_upload (
        id, hashedUserId, objectKey, filename, triggerSource, modelDangerLevel,
        capturedAtEpochMs, sampleRateHz, durationMs, contentType, byteSize
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET
         objectKey = excluded.objectKey,
         filename = excluded.filename,
         triggerSource = excluded.triggerSource,
         modelDangerLevel = excluded.modelDangerLevel,
         capturedAtEpochMs = excluded.capturedAtEpochMs,
         sampleRateHz = excluded.sampleRateHz,
         durationMs = excluded.durationMs,
         contentType = excluded.contentType,
         byteSize = excluded.byteSize`
    ).bind(
      uploadId,
      hashedUserId,
      objectKey,
      normalizedFilename,
      metadata.triggerSource || 'unknown',
      metadata.dangerLevel?.trim() || null,
      metadata.capturedAtEpochMs ?? 0,
      metadata.sampleRateHz ?? 16_000,
      metadata.durationMs ?? 5_000,
      contentType,
      audioBytes.byteLength,
    ).run();

    return c.json({ ok: true, uploadId, objectKey });
  } catch (error: any) {
    console.error('[/training/audio] Upload failed:', error);
    return c.json({ error: error.message || 'Upload failed' }, 502);
  }
});

app.post('/alerts/analysis', requireAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { userId, message } = body;

  if (!userId || !message) {
    return c.json({ error: 'userId and message are required' }, 400);
  }

  try {
    const { results: boundContacts } = await c.env.DB.prepare(
      "SELECT id, chatId FROM contacts WHERE userId = ? AND status = 'Bound'"
    ).bind(userId).all();

    if (!boundContacts || boundContacts.length === 0) {
      return c.json({ ok: true, sent: 0, skipped: true });
    }

    for (const contact of boundContacts as { id: string; chatId: number }[]) {
      await sendTelegramLongMessage(c.env.TELEGRAM_BOT_TOKEN, contact.chatId, message);
      await markTelegramContactAlertSent(c.env.DB, userId, contact.id);
    }
    await markUserAlertSent(c.env.DB, userId);
    return c.json({ ok: true, sent: boundContacts.length });
  } catch (error: any) {
    console.error('[/alerts/analysis] Telegram analysis delivery failed:', error);
    return c.json({ error: error.message }, 502);
  }
});

app.post('/push/alerts', requireAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { userId, title, message, eventId, messageType } = body;

  if (!userId || !message) {
    return c.json({ error: 'userId and message are required' }, 400);
  }

  try {
    const { results } = await c.env.DB.prepare(
      `SELECT
         pc.contactUserId,
         COALESCE(pd.name, 'Aurora contact') AS name,
         COALESCE(pd.deviceName, 'Phone') AS deviceName,
         pd.fcmToken
       FROM push_contacts pc
       INNER JOIN push_devices pd ON pd.userId = pc.contactUserId
       WHERE pc.userId = ?
         AND pc.status = 'Bound'
         AND pd.fcmToken IS NOT NULL
         AND pd.fcmToken != ''`
    ).bind(userId).all<PushDeviceRow>();

    const boundContacts = (results || []) as PushDeviceRow[];
    if (boundContacts.length === 0) {
      return c.json({ ok: true, sent: 0, skipped: true });
    }

    let sentCount = 0;
    const notificationTitle = title || 'SOS Triggered';
    const resolvedEventId = typeof eventId === 'string' && eventId.trim() !== ''
      ? eventId.trim()
      : crypto.randomUUID();
    const resolvedMessageType = typeof messageType === 'string' && messageType.trim() !== ''
      ? messageType.trim()
      : (notificationTitle === 'AI Analysis Result' ? 'ai_analysis' : 'sos');

    for (const contact of boundContacts) {
      try {
        await sendPushNotification(
          c.env,
          contact.fcmToken,
          notificationTitle,
          message,
          userId,
          resolvedEventId,
          resolvedMessageType,
        );
        await markPushContactAlertSent(c.env.DB, userId, contact.contactUserId);
        sentCount += 1;
      } catch (error) {
        console.error(`Push delivery failed for ${contact.contactUserId}:`, error);
      }
    }

    if (sentCount > 0) {
      await markUserAlertSent(c.env.DB, userId);
    }

    return c.json({ ok: true, sent: sentCount, skipped: sentCount === 0 });
  } catch (error: any) {
    console.error('[/push/alerts] Push delivery failed:', error);
    return c.json({ error: error.message }, 502);
  }
});

app.post('/telegram/webhook', async (c) => {
  const body = await c.req.json().catch(() => null);
  const message = body?.message;

  if (!message?.text || !message?.chat?.id) {
    return c.text('OK');
  }

  const trimmedText = message.text.trim();
  if (!trimmedText.startsWith('/start')) {
    return c.text('OK');
  }

  const bindCode = trimmedText.split(/\s+/, 2)[1];
  if (!bindCode) {
    return c.text('OK');
  }

  try {
    const user = await c.env.DB.prepare(
      'SELECT userId FROM users WHERE bindCode = ?'
    ).bind(bindCode).first<{ userId: string }>();

    if (!user) return c.text('OK');

    const contactId = String(message.chat.id);
    const name = [message.from?.first_name, message.from?.last_name].filter(Boolean).join(' ').trim() || 'Telegram user';
    const telegramHandle = sanitizeTelegramHandle(message.from?.username || '');

    await c.env.DB.prepare(
      `INSERT INTO contacts (id, userId, name, telegramHandle, status, chatId, boundAt)
       VALUES (?, ?, ?, ?, 'Bound', ?, CURRENT_TIMESTAMP)
       ON CONFLICT(id, userId) DO UPDATE SET
         name = excluded.name,
         telegramHandle = excluded.telegramHandle,
         status = 'Bound',
         chatId = excluded.chatId`
    ).bind(contactId, user.userId, name, telegramHandle, message.chat.id).run();

    await sendTelegramMessage(
      c.env.TELEGRAM_BOT_TOKEN,
      message.chat.id,
      `Binding complete. You will now receive alarm alerts for user ${user.userId}.`
    );
  } catch (error) {
    console.error('Webhook error:', error);
  }

  return c.text('OK');
});

export default app;
