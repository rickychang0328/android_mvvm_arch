'use strict';

const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const {
  DynamoDBDocumentClient,
  PutCommand,
  DeleteCommand,
} = require('@aws-sdk/lib-dynamodb');
const { verifyToken, getUserId } = require('./shared/jwt');

const JWT_ISSUER = process.env.JWT_ISSUER;
const JWT_AUDIENCE = process.env.JWT_AUDIENCE;
const CONNECTIONS_TABLE = process.env.CONNECTIONS_TABLE_NAME;
const CONNECTION_TTL_SECONDS = 24 * 60 * 60;

const dynamo = DynamoDBDocumentClient.from(new DynamoDBClient({}));

function response(statusCode, body) {
  return { statusCode, body: body ? JSON.stringify(body) : undefined };
}

async function authenticateConnect(event) {
  const token =
    event.queryStringParameters?.token ||
    event.queryStringParameters?.access_token ||
    null;

  if (!token) {
    return { error: response(401, { message: 'Missing token.' }) };
  }

  try {
    const payload = await verifyToken(token, { issuer: JWT_ISSUER, audience: JWT_AUDIENCE });
    const userId = getUserId(payload);
    if (!userId) {
      return { error: response(401, { message: 'Token missing subject.' }) };
    }
    return { userId };
  } catch (err) {
    console.error('WebSocket JWT verification failed:', err.message);
    return { error: response(401, { message: 'Invalid or expired token.' }) };
  }
}

async function handleConnect(event) {
  const auth = await authenticateConnect(event);
  if (auth.error) {
    return auth.error;
  }

  const connectionId = event.requestContext.connectionId;
  const ttl = Math.floor(Date.now() / 1000) + CONNECTION_TTL_SECONDS;

  await dynamo.send(
    new PutCommand({
      TableName: CONNECTIONS_TABLE,
      Item: {
        connectionId,
        userId: auth.userId,
        ttl,
        connectedAt: new Date().toISOString(),
      },
    }),
  );

  return response(200);
}

async function handleDisconnect(event) {
  const connectionId = event.requestContext.connectionId;

  await dynamo.send(
    new DeleteCommand({
      TableName: CONNECTIONS_TABLE,
      Key: { connectionId },
    }),
  );

  return response(200);
}

async function handleDefault(event) {
  const connectionId = event.requestContext.connectionId;
  let action = 'unknown';

  if (event.body) {
    try {
      const body = JSON.parse(event.body);
      action = body.action || action;
    } catch (_err) {
      // ignore malformed client payloads
    }
  }

  console.log(`Default route from ${connectionId}, action=${action}`);
  return response(200, { action, status: 'ok' });
}

exports.handler = async (event) => {
  const routeKey = event.requestContext.routeKey;

  try {
    switch (routeKey) {
      case '$connect':
        return handleConnect(event);
      case '$disconnect':
        return handleDisconnect(event);
      case '$default':
        return handleDefault(event);
      default:
        return response(400, { message: `Unsupported route: ${routeKey}` });
    }
  } catch (err) {
    console.error('realtime-handler error:', err);
    return response(500, { message: 'Internal server error.' });
  }
};
