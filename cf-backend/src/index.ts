import { Hono } from 'hono';

type Bindings = {
  DB: D1Database;
  TRAINING_AUDIO_BUCKET: R2Bucket;
  ALARM_API_TOKEN: string;
  TELEGRAM_BOT_TOKEN: string;
  LINE_CHANNEL_ACCESS_TOKEN?: string;
  LINE_CHANNEL_SECRET?: string;
  FCM_PROJECT_ID?: string;
  FCM_CLIENT_EMAIL?: string;
  FCM_PRIVATE_KEY?: string;
  USER_ID_HASH_SALT?: string;
};

type Variables = {
  authUserId: string;
};

type PushDeviceRow = {
  contactUserId: string;
  name: string;
  deviceName: string;
  fcmToken: string;
};

type LineContactRow = {
  id: string;
  lineUserId: string;
  displayName: string;
  status: string;
};

type TrainingAudioMetadata = {
  triggerSource?: string;
  dangerLevel?: string | null;
  capturedAtEpochMs?: number;
  sampleRateHz?: number;
  durationMs?: number;
};

let cachedGoogleAccessToken: { token: string; expiresAt: number } | null = null;

const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();
const ALERT_AUDIO_PREFIX = 'aurora-alert-audio';
const ALERT_AUDIO_TTL_SECONDS = 7 * 24 * 60 * 60;
const DEFAULT_ALERT_AUDIO_DURATION_MS = 5_000;

const requireBootstrapAuth = async (c: any, next: any) => {
  const sharedToken = (c.env.ALARM_API_TOKEN || '').trim();
  if (!sharedToken) return await next();

  const authHeader = (c.req.header('Authorization') || '').trim();
  if (authHeader !== `Bearer ${sharedToken}`) {
    return c.json({ error: 'Unauthorized' }, 401);
  }
  await next();
};

const requireDeviceAuth = async (c: any, next: any) => {
  const authHeader = (c.req.header('Authorization') || '').trim();
  const token = authHeader.startsWith('Bearer ')
    ? authHeader.slice('Bearer '.length).trim()
    : '';

  if (!token) {
    return c.json({ error: 'Unauthorized' }, 401);
  }

  const tokenHash = await buildDeviceTokenHash(c.env, token);
  const device = await c.env.DB.prepare(
    `SELECT userId
     FROM device_auth
     WHERE tokenHash = ?
       AND revokedAt IS NULL`
  ).bind(tokenHash).first() as { userId: string } | null;

  if (!device?.userId) {
    return c.json({ error: 'Unauthorized' }, 401);
  }

  c.set('authUserId', device.userId);
  await c.env.DB.prepare(
    'UPDATE device_auth SET lastUsedAt = CURRENT_TIMESTAMP WHERE userId = ?'
  ).bind(device.userId).run();
  await next();
};

function requireMatchingUser(c: any, userId: string) {
  const authUserId = c.get('authUserId');
  if (!userId || authUserId !== userId) {
    return c.json({ error: 'Unauthorized userId' }, 403);
  }
  return null;
}

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

async function buildDeviceTokenHash(env: Bindings, token: string) {
  const salt = (env.USER_ID_HASH_SALT || '').trim();
  return await sha256Hex(`device-token:${salt}:${token}`);
}

async function buildInstallSecretHash(env: Bindings, installSecret: string) {
  const salt = (env.USER_ID_HASH_SALT || '').trim();
  return await sha256Hex(`install-secret:${salt}:${installSecret}`);
}

function generateDeviceToken() {
  const bytes = new Uint8Array(32);
  crypto.getRandomValues(bytes);
  return Array.from(bytes)
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('');
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

async function markLineContactAlertSent(db: D1Database, userId: string, lineUserId: string) {
  await db.prepare(
    'UPDATE line_contacts SET lastAlertSentAt = CURRENT_TIMESTAMP WHERE userId = ? AND lineUserId = ?'
  ).bind(userId, lineUserId).run();
}

function splitLineText(text: string, maxLength = 4900) {
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

type LinePushMessage =
  | { type: 'text'; text: string }
  | { type: 'audio'; originalContentUrl: string; duration: number };

async function sendLinePushMessages(
  channelAccessToken: string | undefined,
  lineUserId: string,
  messages: LinePushMessage[],
) {
  const accessToken = (channelAccessToken || '').trim();
  if (!accessToken) throw new Error('Missing LINE_CHANNEL_ACCESS_TOKEN');

  const response = await fetch('https://api.line.me/v2/bot/message/push', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({
      to: lineUserId,
      messages,
    }),
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`LINE push failed: ${response.status} ${body}`);
  }
}

async function sendLinePushMessage(channelAccessToken: string | undefined, lineUserId: string, text: string) {
  await sendLinePushMessages(channelAccessToken, lineUserId, [{ type: 'text', text }]);
}

async function sendLineLongMessage(channelAccessToken: string | undefined, lineUserId: string, text: string) {
  for (const chunk of splitLineText(text)) {
    await sendLinePushMessage(channelAccessToken, lineUserId, chunk);
  }
}

async function sendLineAudioMessage(
  channelAccessToken: string | undefined,
  lineUserId: string,
  audioUrl: string,
  durationMs: number,
  caption: string,
) {
  const messages: LinePushMessage[] = [];
  const trimmedCaption = caption.trim();
  if (trimmedCaption) {
    messages.push({ type: 'text', text: trimmedCaption });
  }
  messages.push({
    type: 'audio',
    originalContentUrl: audioUrl,
    duration: durationMs > 0 ? durationMs : DEFAULT_ALERT_AUDIO_DURATION_MS,
  });

  await sendLinePushMessages(channelAccessToken, lineUserId, messages);
}

function sanitizeAudioFilename(filename: string) {
  const fallback = 'crisis-audio.m4a';
  const trimmed = (filename || fallback).trim();
  const baseName = trimmed.split(/[\\/]/).pop() || fallback;
  const safeName = baseName.replace(/[^A-Za-z0-9._-]/g, '-').replace(/-+/g, '-');
  const withExtension = safeName.toLowerCase().endsWith('.m4a')
    ? safeName
    : `${safeName.replace(/\.[^.]+$/, '')}.m4a`;
  return withExtension || fallback;
}

function getTaipeiDateTimeParts(date = new Date()) {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Taipei',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(date);
  const valueOf = (type: string) => parts.find((part) => part.type === type)?.value || '';

  return {
    dateFolder: `${valueOf('year')}-${valueOf('month')}-${valueOf('day')}`,
    timePrefix: `${valueOf('hour')}${valueOf('minute')}${valueOf('second')}`,
  };
}

function randomHexId(length: number) {
  const bytes = new Uint8Array(Math.ceil(length / 2));
  crypto.getRandomValues(bytes);
  return Array.from(bytes)
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('')
    .slice(0, length);
}

async function storeLineAlertAudio(c: any, audioBase64: string, filename: string) {
  const audioBytes = decodeBase64ToUint8Array(audioBase64);
  const { dateFolder, timePrefix } = getTaipeiDateTimeParts();
  const generatedFilename = `${timePrefix}-${randomHexId(8)}.m4a`;
  const key = `${ALERT_AUDIO_PREFIX}/${dateFolder}/${generatedFilename}`;
  const expiresAt = new Date(Date.now() + ALERT_AUDIO_TTL_SECONDS * 1000).toISOString();

  await c.env.TRAINING_AUDIO_BUCKET.put(key, audioBytes, {
    httpMetadata: {
      contentType: 'audio/mp4',
      cacheControl: 'private, max-age=0, no-store',
    },
    customMetadata: {
      expiresAt,
    },
  });

  const requestUrl = new URL(c.req.url);
  return `${requestUrl.protocol}//${requestUrl.host}/line/audio/${dateFolder}/${encodeURIComponent(generatedFilename)}`;
}

async function serveLineAudioObject(c: any, key: string) {
  const object = await c.env.TRAINING_AUDIO_BUCKET.get(key);
  if (!object) {
    return c.text('Not found', 404);
  }

  const expiresAt = object.customMetadata?.expiresAt;
  if (expiresAt && Date.parse(expiresAt) < Date.now()) {
    return c.text('Audio expired', 410);
  }

  const headers = new Headers();
  object.writeHttpMetadata(headers);
  headers.set('Content-Type', object.httpMetadata?.contentType || 'audio/mp4');
  headers.set('Cache-Control', 'private, max-age=0, no-store');
  headers.set('Content-Length', String(object.size));
  return new Response(object.body, { headers });
}

async function replyLineMessage(channelAccessToken: string | undefined, replyToken: string | undefined, text: string) {
  const accessToken = (channelAccessToken || '').trim();
  if (!accessToken || !replyToken) return;

  const response = await fetch('https://api.line.me/v2/bot/message/reply', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({
      replyToken,
      messages: [
        {
          type: 'text',
          text,
        },
      ],
    }),
  });

  if (!response.ok) {
    const body = await response.text();
    console.error(`LINE reply failed: ${response.status} ${body}`);
  }
}

async function fetchLineDisplayName(channelAccessToken: string | undefined, lineUserId: string) {
  const accessToken = (channelAccessToken || '').trim();
  if (!accessToken || !lineUserId) return '';

  const response = await fetch(
    `https://api.line.me/v2/bot/profile/${encodeURIComponent(lineUserId)}`,
    {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  );

  if (!response.ok) return '';

  const profile: { displayName?: string } = await response.json<{ displayName?: string }>()
    .catch(() => ({}));
  return profile.displayName?.trim() || '';
}

function extractLineBindCode(text: string) {
  const normalized = (text || '').trim().toUpperCase();
  const match = normalized.match(/[A-Z0-9]{12}/);
  return match?.[0] || '';
}

async function verifyLineSignature(channelSecret: string | undefined, rawBody: string, signature: string | null | undefined) {
  const secret = (channelSecret || '').trim();
  if (!secret || !signature) return false;

  const key = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign'],
  );
  const signed = await crypto.subtle.sign(
    'HMAC',
    key,
    new TextEncoder().encode(rawBody),
  );
  const expectedSignature = btoa(String.fromCharCode(...new Uint8Array(signed)));
  return timingSafeEqual(expectedSignature, signature);
}

function timingSafeEqual(left: string, right: string) {
  if (left.length !== right.length) return false;

  let mismatch = 0;
  for (let index = 0; index < left.length; index++) {
    mismatch |= left.charCodeAt(index) ^ right.charCodeAt(index);
  }
  return mismatch === 0;
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
  extraData: Record<string, string> = {},
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
            ...extraData,
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

app.get('/line/audio/:date/:filename', async (c) => {
  const date = c.req.param('date');
  const filename = c.req.param('filename');

  if (
    !/^\d{4}-\d{2}-\d{2}$/.test(date) ||
    !/^\d{6}-[0-9a-f]{8}\.m4a$/i.test(filename)
  ) {
    return c.text('Not found', 404);
  }

  return serveLineAudioObject(c, `${ALERT_AUDIO_PREFIX}/${date}/${filename}`);
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

app.post('/devices/register', requireBootstrapAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const userId = typeof body.userId === 'string' ? body.userId.trim() : '';
  const bindCode = typeof body.bindCode === 'string' ? body.bindCode.trim() : '';
  const deviceName = typeof body.deviceName === 'string' ? body.deviceName.trim() : '';
  const installSecret = typeof body.installSecret === 'string' ? body.installSecret.trim() : '';

  if (!userId || !bindCode || !installSecret) {
    return c.json({ error: 'userId, bindCode, and installSecret are required' }, 400);
  }

  try {
    const existingUser = await c.env.DB.prepare(
      'SELECT bindCode FROM users WHERE userId = ?'
    ).bind(userId).first<{ bindCode: string }>();

    const installSecretHash = await buildInstallSecretHash(c.env, installSecret);
    const existingDevice = await c.env.DB.prepare(
      'SELECT installSecretHash FROM device_auth WHERE userId = ?'
    ).bind(userId).first<{ installSecretHash: string | null }>();

    if (
      existingDevice?.installSecretHash &&
      existingDevice.installSecretHash !== installSecretHash
    ) {
      return c.json({ error: 'Device identity does not match' }, 403);
    }

    if (existingUser?.bindCode && existingUser.bindCode !== bindCode) {
      return c.json({ error: 'Bind code does not match this device identity' }, 409);
    }

    await c.env.DB.prepare(
      `INSERT INTO users (userId, bindCode, deviceName, lastRegisteredAt)
       VALUES (?, ?, ?, CURRENT_TIMESTAMP)
       ON CONFLICT(userId) DO UPDATE SET
         bindCode = excluded.bindCode,
         deviceName = excluded.deviceName,
         lastRegisteredAt = CURRENT_TIMESTAMP`
    ).bind(userId, bindCode, deviceName || 'Phone').run();

    const deviceToken = generateDeviceToken();
    const tokenHash = await buildDeviceTokenHash(c.env, deviceToken);

    await c.env.DB.prepare(
      `INSERT INTO device_auth (userId, tokenHash, installSecretHash, deviceName, lastIssuedAt, revokedAt)
       VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, NULL)
       ON CONFLICT(userId) DO UPDATE SET
         tokenHash = excluded.tokenHash,
         installSecretHash = COALESCE(device_auth.installSecretHash, excluded.installSecretHash),
         deviceName = excluded.deviceName,
         lastIssuedAt = CURRENT_TIMESTAMP,
         revokedAt = NULL`
    ).bind(userId, tokenHash, installSecretHash, deviceName || 'Phone').run();

    return c.json({
      ok: true,
      userId,
      bindCode,
      deviceToken,
    });
  } catch (err: any) {
    console.error('Device register error:', err);
    return c.json({ error: 'Internal server error' }, 500);
  }
});

app.post('/users/register', requireDeviceAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { userId, bindCode, deviceName, contactLabel } = body;

  if (!userId || !bindCode) {
    return c.json({ error: 'userId and bindCode are required' }, 400);
  }

  const authError = requireMatchingUser(c, userId);
  if (authError) return authError;

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

app.post('/push/register-device', requireDeviceAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { userId, name, deviceName, fcmToken } = body;

  if (!userId || !fcmToken) {
    return c.json({ error: 'userId and fcmToken are required' }, 400);
  }

  const authError = requireMatchingUser(c, userId);
  if (authError) return authError;

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

app.post('/push/bind', requireDeviceAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { bindCode, contactUserId } = body;

  if (!bindCode || !contactUserId) {
    return c.json({ error: 'bindCode and contactUserId are required' }, 400);
  }

  const authError = requireMatchingUser(c, contactUserId);
  if (authError) return authError;

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

app.get('/users/:userId/contacts', requireDeviceAuth, async (c) => {
  try {
    const userId = c.req.param('userId');
    const authError = requireMatchingUser(c, userId);
    if (authError) return authError;

    const { results: contacts } = await c.env.DB.prepare(
      'SELECT id, name, telegramHandle, status FROM contacts WHERE userId = ?'
    ).bind(userId).all();

    return c.json({ contacts: contacts || [] });
  } catch (err) {
    return c.json({ error: 'Internal server error' }, 500);
  }
});

app.get('/users/:userId/push-contacts', requireDeviceAuth, async (c) => {
  try {
    const userId = c.req.param('userId');
    const authError = requireMatchingUser(c, userId);
    if (authError) return authError;

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

app.get('/users/:userId/line-contacts', requireDeviceAuth, async (c) => {
  try {
    const userId = c.req.param('userId');
    const authError = requireMatchingUser(c, userId);
    if (authError) return authError;

    const { results: contacts } = await c.env.DB.prepare(
      `SELECT
         lineUserId AS id,
         COALESCE(displayName, 'LINE contact') AS name,
         status AS status
       FROM line_contacts
       WHERE userId = ?
       ORDER BY boundAt DESC, createdAt DESC`
    ).bind(userId).all();

    return c.json({ contacts: contacts || [] });
  } catch (err) {
    console.error('Fetch LINE contacts error:', err);
    return c.json({ error: 'Internal server error' }, 500);
  }
});

app.delete('/users/:userId/contacts/:contactId', requireDeviceAuth, async (c) => {
  try {
    const userId = c.req.param('userId');
    const contactId = c.req.param('contactId');

    if (!userId || !contactId) {
      return c.json({ error: 'userId and contactId are required' }, 400);
    }

    const authError = requireMatchingUser(c, userId);
    if (authError) return authError;

    const result = await c.env.DB.prepare(
      'DELETE FROM contacts WHERE userId = ? AND id = ?'
    ).bind(userId, contactId).run();

    return c.json({ ok: true, deleted: result.meta.changes || 0 });
  } catch (err) {
    console.error('Delete contact error:', err);
    return c.json({ error: 'Internal server error' }, 500);
  }
});

app.delete('/users/:userId/push-contacts/:contactUserId', requireDeviceAuth, async (c) => {
  try {
    const userId = c.req.param('userId');
    const contactUserId = c.req.param('contactUserId');

    if (!userId || !contactUserId) {
      return c.json({ error: 'userId and contactUserId are required' }, 400);
    }

    const authError = requireMatchingUser(c, userId);
    if (authError) return authError;

    const result = await c.env.DB.prepare(
      'DELETE FROM push_contacts WHERE userId = ? AND contactUserId = ?'
    ).bind(userId, contactUserId).run();

    return c.json({ ok: true, deleted: result.meta.changes || 0 });
  } catch (err) {
    console.error('Delete push contact error:', err);
    return c.json({ error: 'Internal server error' }, 500);
  }
});

app.delete('/users/:userId/line-contacts/:lineUserId', requireDeviceAuth, async (c) => {
  try {
    const userId = c.req.param('userId');
    const lineUserId = c.req.param('lineUserId');

    if (!userId || !lineUserId) {
      return c.json({ error: 'userId and lineUserId are required' }, 400);
    }

    const authError = requireMatchingUser(c, userId);
    if (authError) return authError;

    const result = await c.env.DB.prepare(
      'DELETE FROM line_contacts WHERE userId = ? AND lineUserId = ?'
    ).bind(userId, lineUserId).run();

    return c.json({ ok: true, deleted: result.meta.changes || 0 });
  } catch (err) {
    console.error('Delete LINE contact error:', err);
    return c.json({ error: 'Internal server error' }, 500);
  }
});

app.post('/alerts', requireDeviceAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { userId, message } = body;

  if (!userId || !message) {
    return c.json({ error: 'userId and message are required' }, 400);
  }

  const authError = requireMatchingUser(c, userId);
  if (authError) return authError;

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

app.post('/line/alerts', requireDeviceAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { userId, message } = body;

  if (!userId || !message) {
    return c.json({ error: 'userId and message are required' }, 400);
  }

  const authError = requireMatchingUser(c, userId);
  if (authError) return authError;

  try {
    const { results } = await c.env.DB.prepare(
      `SELECT
         lineUserId AS id,
         lineUserId,
         COALESCE(displayName, 'LINE contact') AS displayName,
         status
       FROM line_contacts
       WHERE userId = ?
         AND status = 'Bound'
         AND lineUserId IS NOT NULL
         AND lineUserId != ''`
    ).bind(userId).all<LineContactRow>();

    const boundContacts = (results || []) as LineContactRow[];
    if (boundContacts.length === 0) {
      return c.json({ ok: true, sent: 0, skipped: true });
    }

    let sentCount = 0;
    for (const contact of boundContacts) {
      try {
        await sendLineLongMessage(c.env.LINE_CHANNEL_ACCESS_TOKEN, contact.lineUserId, message);
        await markLineContactAlertSent(c.env.DB, userId, contact.lineUserId);
        sentCount += 1;
      } catch (error) {
        console.error(`LINE delivery failed for ${contact.lineUserId}:`, error);
      }
    }

    if (sentCount > 0) {
      await markUserAlertSent(c.env.DB, userId);
    }

    return c.json({ ok: true, sent: sentCount, skipped: sentCount === 0 });
  } catch (error: any) {
    console.error('[/line/alerts] LINE delivery failed:', error);
    return c.json({ error: error.message }, 502);
  }
});

app.post('/line/alerts/audio-analysis', requireDeviceAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { userId, audioBase64, filename, caption, analysisMessage } = body;
  const durationMs = Number.isFinite(Number(body.durationMs))
    ? Number(body.durationMs)
    : DEFAULT_ALERT_AUDIO_DURATION_MS;

  if (!userId || !audioBase64 || !analysisMessage) {
    return c.json({ error: 'userId, audioBase64, and analysisMessage are required' }, 400);
  }

  const authError = requireMatchingUser(c, userId);
  if (authError) return authError;

  try {
    const { results } = await c.env.DB.prepare(
      `SELECT
         lineUserId AS id,
         lineUserId,
         COALESCE(displayName, 'LINE contact') AS displayName,
         status
       FROM line_contacts
       WHERE userId = ?
         AND status = 'Bound'
         AND lineUserId IS NOT NULL
         AND lineUserId != ''`
    ).bind(userId).all<LineContactRow>();

    const lineContacts = (results || []) as LineContactRow[];
    if (lineContacts.length === 0) {
      return c.json({ ok: true, sent: 0, skipped: true });
    }

    const lineAudioUrl = await storeLineAlertAudio(c, audioBase64, filename || 'crisis-audio.m4a');
    let sentCount = 0;
    for (const contact of lineContacts) {
      const messages: LinePushMessage[] = [];
      const trimmedCaption = String(caption || '').trim();
      if (trimmedCaption) {
        messages.push({ type: 'text', text: trimmedCaption });
      }
      messages.push({
        type: 'audio',
        originalContentUrl: lineAudioUrl,
        duration: durationMs > 0 ? durationMs : DEFAULT_ALERT_AUDIO_DURATION_MS,
      });
      messages.push({ type: 'text', text: String(analysisMessage) });

      await sendLinePushMessages(c.env.LINE_CHANNEL_ACCESS_TOKEN, contact.lineUserId, messages);
      await markLineContactAlertSent(c.env.DB, userId, contact.lineUserId);
      sentCount += 1;
    }

    if (sentCount > 0) {
      await markUserAlertSent(c.env.DB, userId);
    }

    return c.json({ ok: true, sent: sentCount, skipped: sentCount === 0 });
  } catch (error: any) {
    console.error('[/line/alerts/audio-analysis] LINE delivery failed:', error);
    return c.json({ error: error.message }, 502);
  }
});

app.post('/alerts/audio', requireDeviceAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { userId, audioBase64, filename, caption } = body;
  const sendTelegram = body.sendTelegram !== false;
  const sendLine = body.sendLine === true;
  const sendPush = body.sendPush === true;
  const durationMs = Number.isFinite(Number(body.durationMs))
    ? Number(body.durationMs)
    : DEFAULT_ALERT_AUDIO_DURATION_MS;
  const pushEventId = typeof body.pushEventId === 'string' && body.pushEventId.trim() !== ''
    ? body.pushEventId.trim()
    : crypto.randomUUID();
  const pushTitle = typeof body.pushTitle === 'string' && body.pushTitle.trim() !== ''
    ? body.pushTitle.trim()
    : 'Audio Evidence';
  const pushMessage = typeof body.pushMessage === 'string' && body.pushMessage.trim() !== ''
    ? body.pushMessage.trim()
    : (caption || '5-second crisis audio clip captured by Aurora Security.');

  if (!userId || !audioBase64) {
    return c.json({ error: 'userId and audioBase64 are required' }, 400);
  }

  const authError = requireMatchingUser(c, userId);
  if (authError) return authError;

  if (!sendTelegram && !sendLine && !sendPush) {
    return c.json({ ok: true, sent: 0, skipped: true });
  }

  try {
    const telegramContacts = sendTelegram
      ? ((await c.env.DB.prepare(
        "SELECT id, chatId FROM contacts WHERE userId = ? AND status = 'Bound'"
      ).bind(userId).all()).results || []) as { id: string; chatId: number }[]
      : [];

    const lineContacts = sendLine
      ? ((await c.env.DB.prepare(
        `SELECT
           lineUserId AS id,
           lineUserId,
           COALESCE(displayName, 'LINE contact') AS displayName,
           status
         FROM line_contacts
         WHERE userId = ?
           AND status = 'Bound'
           AND lineUserId IS NOT NULL
           AND lineUserId != ''`
      ).bind(userId).all<LineContactRow>()).results || []) as LineContactRow[]
      : [];

    const pushContacts = sendPush
      ? ((await c.env.DB.prepare(
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
      ).bind(userId).all<PushDeviceRow>()).results || []) as PushDeviceRow[]
      : [];

    if (telegramContacts.length === 0 && lineContacts.length === 0 && pushContacts.length === 0) {
      return c.json({ ok: true, sent: 0, skipped: true });
    }

    let telegramSent = 0;
    let lineSent = 0;
    let pushSent = 0;

    for (const contact of telegramContacts) {
      await sendTelegramAudio(
        c.env.TELEGRAM_BOT_TOKEN,
        contact.chatId,
        audioBase64,
        filename || 'crisis-audio.m4a',
        caption || ''
      );
      await markTelegramContactAlertSent(c.env.DB, userId, contact.id);
      telegramSent += 1;
    }

    let lineAudioUrl = '';
    if (lineContacts.length > 0 || pushContacts.length > 0) {
      lineAudioUrl = await storeLineAlertAudio(c, audioBase64, filename || 'crisis-audio.m4a');
    }

    for (const contact of lineContacts) {
      await sendLineAudioMessage(
        c.env.LINE_CHANNEL_ACCESS_TOKEN,
        contact.lineUserId,
        lineAudioUrl,
        durationMs,
        caption || ''
      );
      await markLineContactAlertSent(c.env.DB, userId, contact.lineUserId);
      lineSent += 1;
    }

    for (const contact of pushContacts) {
      await sendPushNotification(
        c.env,
        contact.fcmToken,
        pushTitle,
        pushMessage,
        userId,
        pushEventId,
        'audio_evidence',
        {
          audioUrl: lineAudioUrl,
          durationMs: String(durationMs > 0 ? durationMs : DEFAULT_ALERT_AUDIO_DURATION_MS),
        },
      );
      await markPushContactAlertSent(c.env.DB, userId, contact.contactUserId);
      pushSent += 1;
    }

    const sent = telegramSent + lineSent + pushSent;
    if (sent > 0) {
      await markUserAlertSent(c.env.DB, userId);
    }
    return c.json({ ok: true, sent, telegramSent, lineSent, pushSent, skipped: sent === 0 });
  } catch (error: any) {
    console.error('[/alerts/audio] Audio delivery failed:', error);
    return c.json({ error: error.message }, 502);
  }
});

app.post('/training/audio', requireDeviceAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const userId = typeof body.userId === 'string' ? body.userId.trim() : '';
  const audioBase64 = typeof body.audioBase64 === 'string' ? body.audioBase64.trim() : '';
  const filename = typeof body.filename === 'string' ? body.filename.trim() : '';
  const metadata = (body.metadata || {}) as TrainingAudioMetadata;

  if (!userId || !audioBase64) {
    return c.json({ error: 'userId and audioBase64 are required' }, 400);
  }

  const authError = requireMatchingUser(c, userId);
  if (authError) return authError;

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

app.post('/alerts/analysis', requireDeviceAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { userId, message } = body;

  if (!userId || !message) {
    return c.json({ error: 'userId and message are required' }, 400);
  }

  const authError = requireMatchingUser(c, userId);
  if (authError) return authError;

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

app.post('/push/alerts', requireDeviceAuth, async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const {
    userId,
    title,
    message,
    eventId,
    messageType,
    sosMessage,
    sosDate,
    sosTime,
    sosDeviceName,
    sosMobileNumber,
    sosCurrentSoundLevel,
    sosLocationLabel,
    sosLocationLink,
  } = body;

  if (!userId || !message) {
    return c.json({ error: 'userId and message are required' }, 400);
  }

  const authError = requireMatchingUser(c, userId);
  if (authError) return authError;

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
    const structuredAlertData = {
      sosMessage: typeof sosMessage === 'string' ? sosMessage : '',
      sosDate: typeof sosDate === 'string' ? sosDate : '',
      sosTime: typeof sosTime === 'string' ? sosTime : '',
      sosDeviceName: typeof sosDeviceName === 'string' ? sosDeviceName : '',
      sosMobileNumber: typeof sosMobileNumber === 'string' ? sosMobileNumber : '',
      sosCurrentSoundLevel: typeof sosCurrentSoundLevel === 'string' ? sosCurrentSoundLevel : '',
      sosLocationLabel: typeof sosLocationLabel === 'string' ? sosLocationLabel : '',
      sosLocationLink: typeof sosLocationLink === 'string' ? sosLocationLink : '',
    };

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
          structuredAlertData,
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

app.post('/line/webhook', async (c) => {
  const rawBody = await c.req.text();
  const signature = c.req.header('X-Line-Signature');
  const signatureOk = await verifyLineSignature(c.env.LINE_CHANNEL_SECRET, rawBody, signature);
  if (!signatureOk) {
    return c.text('Invalid signature', 401);
  }

  const body = JSON.parse(rawBody || '{}') as any;
  const events = Array.isArray(body?.events) ? body.events : [];

  for (const event of events) {
    if (event?.type !== 'message' || event?.message?.type !== 'text') {
      continue;
    }

    const lineUserId = typeof event?.source?.userId === 'string'
      ? event.source.userId
      : '';
    const bindCode = extractLineBindCode(event.message.text || '');

    if (!lineUserId) {
      continue;
    }

    if (!bindCode) {
      await replyLineMessage(
        c.env.LINE_CHANNEL_ACCESS_TOKEN,
        event.replyToken,
        'Please send your Aurora LINE binding code to bind alerts.',
      );
      continue;
    }

    try {
      const user = await c.env.DB.prepare(
        'SELECT userId FROM users WHERE bindCode = ?'
      ).bind(bindCode).first<{ userId: string }>();

      if (!user) {
        await replyLineMessage(
          c.env.LINE_CHANNEL_ACCESS_TOKEN,
          event.replyToken,
          'Aurora binding code not found. Please check the code and try again.',
        );
        continue;
      }

      const displayName = await fetchLineDisplayName(
        c.env.LINE_CHANNEL_ACCESS_TOKEN,
        lineUserId,
      );

      await c.env.DB.prepare(
        `INSERT INTO line_contacts (lineUserId, userId, displayName, status, boundAt)
         VALUES (?, ?, ?, 'Bound', CURRENT_TIMESTAMP)
         ON CONFLICT(lineUserId, userId) DO UPDATE SET
           displayName = excluded.displayName,
           status = 'Bound',
           boundAt = CURRENT_TIMESTAMP`
      ).bind(lineUserId, user.userId, displayName || 'LINE contact').run();

      await replyLineMessage(
        c.env.LINE_CHANNEL_ACCESS_TOKEN,
        event.replyToken,
        'Binding complete. You will now receive Aurora SOS alerts in LINE.',
      );
    } catch (error) {
      console.error('LINE webhook binding error:', error);
      await replyLineMessage(
        c.env.LINE_CHANNEL_ACCESS_TOKEN,
        event.replyToken,
        'Aurora could not bind this LINE account. Please try again later.',
      );
    }
  }

  return c.text('OK');
});

export default app;
