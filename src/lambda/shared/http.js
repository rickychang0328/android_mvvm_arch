'use strict';

function jsonResponse(statusCode, body) {
  return {
    statusCode,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  };
}

function noContent() {
  return { statusCode: 204, headers: {}, body: '' };
}

function parseBody(event) {
  if (!event.body) {
    return {};
  }

  const raw = event.isBase64Encoded
    ? Buffer.from(event.body, 'base64').toString('utf8')
    : event.body;

  return JSON.parse(raw);
}

function getBearerToken(event) {
  const header =
    event.headers?.authorization ||
    event.headers?.Authorization ||
    '';

  if (!header.startsWith('Bearer ')) {
    return null;
  }

  return header.slice('Bearer '.length).trim();
}

function getHttpMethodAndPath(event) {
  const method = event.requestContext?.http?.method || event.httpMethod || 'GET';
  const path = event.requestContext?.http?.path || event.rawPath || event.path || '/';
  return { method, path };
}

module.exports = {
  jsonResponse,
  noContent,
  parseBody,
  getBearerToken,
  getHttpMethodAndPath,
};
