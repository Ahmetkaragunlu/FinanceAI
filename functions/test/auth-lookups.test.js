'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { firestoreMemory } = require('./support/firestore-memory');
const { authLookups } = require('../src/auth-lookups');
const { AuthLookupError, AuthLookupErrorCode, authLookupFailure } = require('../src/auth-errors');
const seed = { 'users/A': { email: 'person@example.test', firstName: 'First', lastName: 'Last', fcmTokens: ['private-token'] } };
const context = { app: { appId: 'app' }, rawRequest: { ip: '127.0.0.1' } };
const auth = { getUserByEmail: async email => {
    if (email === 'person@example.test') return { uid: 'A' };
    throw Object.assign(new Error('Not found'), { code: 'auth/user-not-found' });
} };
test('pre-login lookups return only a boolean, never private profile fields', async () => {
    const result = await authLookups(firestoreMemory(seed), auth, () => 0).check({ email: 'person@example.test' }, context, false);
    assert.deepEqual(result, { found: true });
});
test('reset identity requires the existing first/last name checks', async () => {
    const lookup = authLookups(firestoreMemory(seed), auth, () => 0);
    assert.deepEqual(await lookup.check({ email: 'person@example.test', firstName: 'Wrong', lastName: 'Last' }, context, true), { found: false });
    assert.deepEqual(await lookup.check({ email: 'person@example.test', firstName: 'First', lastName: 'Last' }, context, true), { found: true });
});
test('App Check is required and brute force is bounded per minute', async () => {
    const lookup = authLookups(firestoreMemory(seed), auth, () => 0);
    await assert.rejects(lookup.check({ email: 'person@example.test' }, {}, false),
        error => error instanceof AuthLookupError && error.code === AuthLookupErrorCode.APP_CHECK_REQUIRED);
    for (let i = 0; i < 10; i++) await lookup.check({ email: 'person@example.test' }, context, false);
    await assert.rejects(lookup.check({ email: 'person@example.test' }, context, false),
        error => error instanceof AuthLookupError && error.code === AuthLookupErrorCode.RATE_LIMITED);
});

test('lookup response mapping uses typed codes even when diagnostic messages change', () => {
    const cases = [
        [AuthLookupErrorCode.APP_CHECK_REQUIRED, 'failed-precondition', 'App verification required'],
        [AuthLookupErrorCode.RATE_LIMITED, 'resource-exhausted', 'Try again later'],
        [AuthLookupErrorCode.INVALID_INPUT, 'invalid-argument', 'Invalid account check']
    ];
    for (const [code, responseCode, message] of cases) {
        const error = new AuthLookupError(code);
        error.message = 'Different diagnostics, never part of the client contract';
        assert.deepEqual(authLookupFailure(error), { code: responseCode, message });
    }
});
test('unknown and message-only errors remain generic rather than impersonating lookup errors', () => {
    for (const error of [new Error('RATE_LIMITED'), Object.assign(new Error('private backend details'),
        { code: AuthLookupErrorCode.INVALID_INPUT })]) {
        assert.deepEqual(authLookupFailure(error), { code: 'internal', message: 'Account check failed' });
    }
});
test('invalid lookup input is rejected by type and code without requesting an Auth profile', async () => {
    const unexpectedAuth = { getUserByEmail: async () => assert.fail('Invalid input must not reach Auth') };
    for (const [data, reset] of [[{ email: 'invalid' }, false],
        [{ email: 'person@example.test', firstName: 10, lastName: 'Last' }, true]]) {
        await assert.rejects(authLookups(firestoreMemory(seed), unexpectedAuth, () => 0).check(data, context, reset),
            error => error instanceof AuthLookupError && error.code === AuthLookupErrorCode.INVALID_INPUT);
    }
});
test('Auth invalid-email codes are converted but infrastructure errors retain identity', async () => {
    const invalid = { getUserByEmail: async () => { throw Object.assign(new Error('SDK diagnostics'), { code: 'auth/invalid-email' }); } };
    await assert.rejects(authLookups(firestoreMemory(seed), invalid, () => 0).check({ email: 'a@b' }, context, false),
        error => error instanceof AuthLookupError && error.code === AuthLookupErrorCode.INVALID_INPUT);
    const failure = Object.assign(new Error('temporary outage'), { code: 'auth/internal-error' });
    const unavailable = { getUserByEmail: async () => { throw failure; } };
    await assert.rejects(authLookups(firestoreMemory(seed), unavailable, () => 0).check({ email: 'a@b' }, context, false),
        error => error === failure);
});
test('rate-limit failures do not commit increments and a new minute reopens the existing window', async () => {
    const db = firestoreMemory(seed);
    let now = 0;
    const lookup = authLookups(db, auth, () => now);
    for (let i = 0; i < 10; i++) await lookup.check({ email: 'person@example.test' }, context, false);
    await assert.rejects(lookup.check({ email: 'person@example.test' }, context, false),
        error => error.code === AuthLookupErrorCode.RATE_LIMITED);
    assert.equal([...db.values.entries()].find(([path]) => path.startsWith('auth_lookup_limits/'))[1].count, 10);
    now = 60_000;
    assert.deepEqual(await lookup.check({ email: 'person@example.test' }, context, false), { found: true });
});
