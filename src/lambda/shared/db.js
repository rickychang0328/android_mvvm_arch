'use strict';

const { Pool } = require('pg');
const { SecretsManagerClient, GetSecretValueCommand } = require('@aws-sdk/client-secrets-manager');

let pool;
let cachedSecret;

async function getDbCredentials() {
  if (cachedSecret) {
    return cachedSecret;
  }

  const client = new SecretsManagerClient({});
  const response = await client.send(
    new GetSecretValueCommand({ SecretId: process.env.DB_SECRET_ARN }),
  );

  cachedSecret = JSON.parse(response.SecretString);
  return cachedSecret;
}

async function getPool() {
  if (pool) {
    return pool;
  }

  const credentials = await getDbCredentials();
  pool = new Pool({
    host: process.env.DB_ENDPOINT,
    port: Number(process.env.DB_PORT || 5432),
    database: process.env.DB_NAME,
    user: credentials.username,
    password: credentials.password,
    max: 2,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 10000,
    ssl: { rejectUnauthorized: false },
  });

  return pool;
}

module.exports = { getPool };
