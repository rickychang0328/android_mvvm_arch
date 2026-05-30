'use strict';

const { SQSClient, SendMessageCommand } = require('@aws-sdk/client-sqs');
const { getPool } = require('./shared/db');
const { verifyToken, hasScope, getUserId } = require('./shared/jwt');
const {
  jsonResponse,
  noContent,
  parseBody,
  getBearerToken,
  getHttpMethodAndPath,
} = require('./shared/http');

const sqsClient = new SQSClient({});

const JWT_ISSUER = process.env.JWT_ISSUER;
const JWT_AUDIENCE = process.env.JWT_AUDIENCE;
const NOTIFICATION_SERVICE_SCOPE = process.env.NOTIFICATION_SERVICE_SCOPE || 'notifications:send';
const SQS_QUEUE_URL = process.env.SQS_NOTIFICATION_QUEUE_URL;

async function authenticate(event) {
  const token = getBearerToken(event);
  if (!token) {
    return { error: jsonResponse(401, { error: 'unauthorized', message: 'Missing bearer token.' }) };
  }

  try {
    const payload = await verifyToken(token, { issuer: JWT_ISSUER, audience: JWT_AUDIENCE });
    const userId = getUserId(payload);
    if (!userId) {
      return { error: jsonResponse(401, { error: 'unauthorized', message: 'Token missing subject.' }) };
    }
    return { payload, userId };
  } catch (err) {
    console.error('JWT verification failed:', err.message);
    return { error: jsonResponse(401, { error: 'unauthorized', message: 'Invalid or expired token.' }) };
  }
}

async function authenticateWithScope(event, requiredScope) {
  const auth = await authenticate(event);
  if (auth.error) {
    return auth;
  }

  if (!hasScope(auth.payload, requiredScope)) {
    return {
      error: jsonResponse(403, {
        error: 'forbidden',
        message: `Missing required scope: ${requiredScope}`,
      }),
    };
  }

  return auth;
}

function mapNotificationRow(row) {
  return {
    id: row.id,
    title: row.title,
    body: row.body,
    type: row.type,
    is_read: row.read_at != null,
    created_at: new Date(row.created_at).getTime(),
  };
}

async function handleRegisterToken(event, userId) {
  const body = parseBody(event);
  const token = body.token?.trim();
  const platform = (body.platform || 'android').trim();

  if (!token) {
    return jsonResponse(400, { error: 'bad_request', message: 'token is required.' });
  }

  const pool = await getPool();
  await pool.query(
    `INSERT INTO device_tokens (user_id, token, platform, updated_at)
     VALUES ($1, $2, $3, now())
     ON CONFLICT (user_id, token)
     DO UPDATE SET platform = EXCLUDED.platform, updated_at = now()`,
    [userId, token, platform],
  );

  return noContent();
}

async function handleInternalSend(event) {
  const auth = await authenticateWithScope(event, NOTIFICATION_SERVICE_SCOPE);
  if (auth.error) {
    return auth.error;
  }

  const body = parseBody(event);
  const userId = body.user_id?.trim();
  const title = body.title?.trim();
  const notificationBody = body.body?.trim() ?? '';
  const type = (body.type || 'general').trim();
  const data = body.data ?? null;

  if (!userId || !title) {
    return jsonResponse(400, { error: 'bad_request', message: 'user_id and title are required.' });
  }

  const pool = await getPool();
  const insertResult = await pool.query(
    `INSERT INTO notifications (user_id, title, body, type, data)
     VALUES ($1, $2, $3, $4, $5)
     RETURNING id, user_id, title, body, type, data, created_at`,
    [userId, title, notificationBody, type, data ? JSON.stringify(data) : null],
  );

  const notification = insertResult.rows[0];

  await sqsClient.send(
    new SendMessageCommand({
      QueueUrl: SQS_QUEUE_URL,
      MessageBody: JSON.stringify({
        notificationId: notification.id,
        userId: notification.user_id,
        title: notification.title,
        body: notification.body,
        type: notification.type,
        data: notification.data,
        createdAt: notification.created_at,
      }),
    }),
  );

  return jsonResponse(202, {
    id: notification.id,
    user_id: notification.user_id,
    status: 'queued',
  });
}

async function handleGetNotifications(event, userId) {
  const page = Math.max(1, Number(event.queryStringParameters?.page || 1));
  const pageSize = Math.max(1, Math.min(100, Number(event.queryStringParameters?.pageSize || 20)));
  const offset = (page - 1) * pageSize;

  const pool = await getPool();
  const [itemsResult, countResult] = await Promise.all([
    pool.query(
      `SELECT id, title, body, type, read_at, created_at
       FROM notifications
       WHERE user_id = $1
       ORDER BY created_at DESC
       LIMIT $2 OFFSET $3`,
      [userId, pageSize, offset],
    ),
    pool.query('SELECT COUNT(*)::int AS total FROM notifications WHERE user_id = $1', [userId]),
  ]);

  const total = countResult.rows[0].total;
  const hasMore = offset + itemsResult.rows.length < total;

  return jsonResponse(200, {
    items: itemsResult.rows.map(mapNotificationRow),
    next_page: hasMore ? page + 1 : null,
    has_more: hasMore,
  });
}

async function handleMarkRead(event, userId, notificationId) {
  const pool = await getPool();
  const result = await pool.query(
    `UPDATE notifications
     SET read_at = COALESCE(read_at, now())
     WHERE id = $1 AND user_id = $2
     RETURNING id`,
    [notificationId, userId],
  );

  if (result.rowCount === 0) {
    return jsonResponse(404, { error: 'not_found', message: 'Notification not found.' });
  }

  return noContent();
}

async function handleMarkOpen(event, userId, notificationId) {
  const pool = await getPool();
  const result = await pool.query(
    `UPDATE notifications
     SET opened_at = COALESCE(opened_at, now())
     WHERE id = $1 AND user_id = $2
     RETURNING id`,
    [notificationId, userId],
  );

  if (result.rowCount === 0) {
    return jsonResponse(404, { error: 'not_found', message: 'Notification not found.' });
  }

  return noContent();
}

async function handleMarkAllRead(_event, userId) {
  const pool = await getPool();
  await pool.query(
    `UPDATE notifications
     SET read_at = now()
     WHERE user_id = $1 AND read_at IS NULL`,
    [userId],
  );

  return noContent();
}

function matchRoute(method, path) {
  if (method === 'POST' && path === '/api/v1/push/register-token') {
    return { name: 'registerToken' };
  }
  if (method === 'POST' && path === '/api/v1/internal/notifications/send') {
    return { name: 'internalSend' };
  }
  if (method === 'GET' && path === '/api/v1/notifications') {
    return { name: 'getNotifications' };
  }
  if (method === 'POST' && path === '/api/v1/notifications/read-all') {
    return { name: 'markAllRead' };
  }

  const readMatch = path.match(/^\/api\/v1\/notifications\/([^/]+)\/read$/);
  if (method === 'PATCH' && readMatch) {
    return { name: 'markRead', id: readMatch[1] };
  }

  const openMatch = path.match(/^\/api\/v1\/notifications\/([^/]+)\/open$/);
  if (method === 'POST' && openMatch) {
    return { name: 'markOpen', id: openMatch[1] };
  }

  return null;
}

exports.handler = async (event) => {
  try {
    const { method, path } = getHttpMethodAndPath(event);
    const route = matchRoute(method, path);

    if (!route) {
      return jsonResponse(404, { error: 'not_found', message: 'Route not found.' });
    }

    if (route.name === 'internalSend') {
      return handleInternalSend(event);
    }

    const auth = await authenticate(event);
    if (auth.error) {
      return auth.error;
    }

    switch (route.name) {
      case 'registerToken':
        return handleRegisterToken(event, auth.userId);
      case 'getNotifications':
        return handleGetNotifications(event, auth.userId);
      case 'markRead':
        return handleMarkRead(event, auth.userId, route.id);
      case 'markOpen':
        return handleMarkOpen(event, auth.userId, route.id);
      case 'markAllRead':
        return handleMarkAllRead(event, auth.userId);
      default:
        return jsonResponse(404, { error: 'not_found', message: 'Route not found.' });
    }
  } catch (err) {
    console.error('api-handler error:', err);
    return jsonResponse(500, { error: 'internal_error', message: 'Unexpected server error.' });
  }
};
