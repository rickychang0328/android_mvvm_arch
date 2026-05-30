'use strict';

const {
  ApiGatewayManagementApiClient,
  PostToConnectionCommand,
  GoneException,
} = require('@aws-sdk/client-apigatewaymanagementapi');
const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const {
  DynamoDBDocumentClient,
  QueryCommand,
  DeleteCommand,
} = require('@aws-sdk/lib-dynamodb');
const { getPool } = require('./shared/db');

const CONNECTIONS_TABLE = process.env.CONNECTIONS_TABLE_NAME;
const MANAGEMENT_ENDPOINT = process.env.WEBSOCKET_MANAGEMENT_ENDPOINT;

const dynamo = DynamoDBDocumentClient.from(new DynamoDBClient({}));

function createManagementClient() {
  return new ApiGatewayManagementApiClient({ endpoint: MANAGEMENT_ENDPOINT });
}

async function getConnectionsForUser(userId) {
  const response = await dynamo.send(
    new QueryCommand({
      TableName: CONNECTIONS_TABLE,
      IndexName: 'userId-index',
      KeyConditionExpression: 'userId = :userId',
      ExpressionAttributeValues: { ':userId': userId },
    }),
  );

  return response.Items || [];
}

async function deleteConnection(connectionId) {
  await dynamo.send(
    new DeleteCommand({
      TableName: CONNECTIONS_TABLE,
      Key: { connectionId },
    }),
  );
}

async function postToConnection(client, connectionId, payload) {
  try {
    await client.send(
      new PostToConnectionCommand({
        ConnectionId: connectionId,
        Data: Buffer.from(JSON.stringify(payload)),
      }),
    );
    return true;
  } catch (err) {
    if (err instanceof GoneException || err.name === 'GoneException' || err.$metadata?.httpStatusCode === 410) {
      await deleteConnection(connectionId);
      return false;
    }
    throw err;
  }
}

async function processRecord(record) {
  const message = JSON.parse(record.body);
  const { notificationId, userId, title, body, type, data } = message;

  if (!notificationId || !userId) {
    throw new Error('Invalid SQS message: notificationId and userId are required.');
  }

  const pool = await getPool();
  await pool.query(
    `UPDATE notifications
     SET delivered_at = COALESCE(delivered_at, now())
     WHERE id = $1 AND user_id = $2`,
    [notificationId, userId],
  );

  const connections = await getConnectionsForUser(userId);
  const payload = {
    action: 'notification',
    notification: {
      id: notificationId,
      title,
      body,
      type: type || 'general',
      data: data || null,
    },
  };

  if (connections.length === 0) {
    console.log(`No active WebSocket connections for user ${userId}`);
    return;
  }

  const managementClient = createManagementClient();
  await Promise.all(
    connections.map((item) => postToConnection(managementClient, item.connectionId, payload)),
  );
}

exports.handler = async (event) => {
  const batchItemFailures = [];

  for (const record of event.Records || []) {
    try {
      await processRecord(record);
    } catch (err) {
      console.error('Failed to process SQS record:', record.messageId, err);
      batchItemFailures.push({ itemIdentifier: record.messageId });
    }
  }

  return { batchItemFailures };
};
