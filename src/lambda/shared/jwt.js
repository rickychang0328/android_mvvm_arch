'use strict';

const jwt = require('jsonwebtoken');
const jwksClient = require('jwks-rsa');

let jwks;

function getJwksClient(issuer) {
  if (!jwks) {
    const normalizedIssuer = issuer.replace(/\/$/, '');
    jwks = jwksClient({
      jwksUri: `${normalizedIssuer}/.well-known/jwks.json`,
      cache: true,
      cacheMaxAge: 600000,
      rateLimit: true,
      jwksRequestsPerMinute: 10,
    });
  }
  return jwks;
}

function getSigningKey(header, issuer) {
  return new Promise((resolve, reject) => {
    getJwksClient(issuer).getSigningKey(header.kid, (err, key) => {
      if (err) {
        reject(err);
        return;
      }
      resolve(key.getPublicKey());
    });
  });
}

async function verifyToken(token, { issuer, audience }) {
  const decoded = jwt.decode(token, { complete: true });
  if (!decoded || !decoded.header?.kid) {
    throw new Error('Invalid token');
  }

  const signingKey = await getSigningKey(decoded.header, issuer);
  return jwt.verify(token, signingKey, {
    issuer,
    audience,
    algorithms: ['RS256', 'ES256', 'RS384', 'ES384'],
  });
}

function extractScopes(payload) {
  const scopes = new Set();

  if (typeof payload.scope === 'string') {
    payload.scope.split(/\s+/).filter(Boolean).forEach((s) => scopes.add(s));
  } else if (Array.isArray(payload.scope)) {
    payload.scope.forEach((s) => scopes.add(String(s)));
  }

  if (typeof payload.scp === 'string') {
    payload.scp.split(/\s+/).filter(Boolean).forEach((s) => scopes.add(s));
  } else if (Array.isArray(payload.scp)) {
    payload.scp.forEach((s) => scopes.add(String(s)));
  }

  return scopes;
}

function hasScope(payload, requiredScope) {
  return extractScopes(payload).has(requiredScope);
}

function getUserId(payload) {
  return payload.sub || payload.user_id || payload.userId || null;
}

module.exports = {
  verifyToken,
  hasScope,
  getUserId,
};
