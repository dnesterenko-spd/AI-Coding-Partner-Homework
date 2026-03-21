'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');

const { getUserById } = require('../src/controllers/userController');

function makeReq(id) {
  return { params: { id } };
}

function makeRes() {
  const res = { statusCode: 200, body: null };
  res.status = (code) => { res.statusCode = code; return res; };
  res.json = (data) => { res.body = data; return res; };
  return res;
}

// Happy path: numeric string ID that matches an existing user
test('getUserById returns the correct user when a valid numeric string ID is provided', async () => {
  const req = makeReq('123');
  const res = makeRes();

  await getUserById(req, res);

  assert.equal(res.statusCode, 200);
  assert.deepEqual(res.body, { id: 123, name: 'Alice Smith', email: 'alice@example.com' });
});

// Regression: before the fix "456" !== 456 and the user was never found
test('getUserById finds the user when req.params.id is a numeric string (regression: type mismatch)', async () => {
  const req = makeReq('456');
  const res = makeRes();

  await getUserById(req, res);

  assert.equal(res.statusCode, 200);
  assert.deepEqual(res.body, { id: 456, name: 'Bob Johnson', email: 'bob@example.com' });
});

// Edge case: valid integer with no matching record returns 404
test('getUserById returns 404 when the parsed ID does not match any user', async () => {
  const req = makeReq('999');
  const res = makeRes();

  await getUserById(req, res);

  assert.equal(res.statusCode, 404);
  assert.deepEqual(res.body, { error: 'User not found' });
});

// Edge case: non-numeric input produces NaN after parseInt → no match → 404
test('getUserById returns 404 when req.params.id is non-numeric', async () => {
  const req = makeReq('abc');
  const res = makeRes();

  await getUserById(req, res);

  assert.equal(res.statusCode, 404);
  assert.deepEqual(res.body, { error: 'User not found' });
});

// Edge case: empty string produces NaN after parseInt → 404
test('getUserById returns 404 when req.params.id is an empty string', async () => {
  const req = makeReq('');
  const res = makeRes();

  await getUserById(req, res);

  assert.equal(res.statusCode, 404);
  assert.deepEqual(res.body, { error: 'User not found' });
});