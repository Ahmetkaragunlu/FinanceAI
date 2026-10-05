'use strict';

const { test, after, mock } = require('node:test');
const assert = require('node:assert/strict');
const { EventEmitter } = require('node:events');
const admin = require('firebase-admin');
const { firestoreMemory } = require('./support/firestore-memory');
const { ScheduleError, ScheduleErrorCode } = require('../src/schedule-errors');

// Load the real v1 export adapters against a demo project; every data/messaging operation is fake.
const previousConfig = process.env.FIREBASE_CONFIG;
const previousProject = process.env.GCLOUD_PROJECT;
process.env.FIREBASE_CONFIG = JSON.stringify({ projectId: 'demo-financeai' });
process.env.GCLOUD_PROJECT = 'demo-financeai';
let handlers;
try { handlers = require('../index'); }
finally {
    if (previousConfig === undefined) delete process.env.FIREBASE_CONFIG;
    else process.env.FIREBASE_CONFIG = previousConfig;
}
const db = admin.firestore();
mock.method(db, 'collection', () => assert.fail('Unstubbed Firestore access is forbidden in handler tests'));
mock.getter(admin, 'messaging', () => () => assert.fail('Unstubbed FCM access is forbidden in handler tests'));
after(async () => {
    mock.restoreAll();
    await admin.app().delete();
    if (previousProject === undefined) delete process.env.GCLOUD_PROJECT;
    else process.env.GCLOUD_PROJECT = previousProject;
});

test('pre-login adapters retain App Check failure response without reaching data storage', async () => {
    for (const name of ['checkRegisteredAccount', 'checkPasswordResetIdentity']) {
        await assert.rejects(handlers[name].run({ email: 'fixture@example.test' }, {}),
            error => error.code === 'failed-precondition' && error.message === 'App verification required');
    }
});
test('account adapter preserves invalid-input/rate-limit wire responses and hides unknown backend diagnostics', async t => {
    const memory = firestoreMemory({});
    t.mock.method(db, 'collection', memory.collection);
    t.mock.method(db, 'runTransaction', memory.runTransaction);
    const context = { app: { appId: 'fixture-app' }, rawRequest: { ip: '127.0.0.1' } };
    await assert.rejects(handlers.checkRegisteredAccount.run({ email: 'invalid' }, context),
        error => error.code === 'invalid-argument' && error.message === 'Invalid account check');
    t.mock.method(admin.auth(), 'getUserByEmail', async () => { throw new Error('RATE_LIMITED'); });
    await assert.rejects(handlers.checkRegisteredAccount.run({ email: 'fixture@example.test' }, context),
        error => error.code === 'internal' && error.message === 'Account check failed');
    const key = [...memory.values.keys()].find(path => path.startsWith('auth_lookup_limits/'));
    memory.values.set(key, { window: Math.floor(Date.now() / 60_000), count: 10 });
    await assert.rejects(handlers.checkRegisteredAccount.run({ email: 'fixture@example.test' }, context),
        error => error.code === 'resource-exhausted' && error.message === 'Try again later');
});
test('both restore adapters retain authentication/device/cursor error responses', async t => {
    t.mock.method(db, 'collection', name => {
        assert.equal(name, 'users');
        return { doc: uid => { assert.equal(uid, 'A');
            return { get: async () => ({ data: () => ({ fcmTokens: ['fixture-token'] }) }) };
        } };
    });
    for (const name of ['sendPendingNotifications', 'restoreScheduleState']) {
        await assert.rejects(handlers[name].run({}, {}),
            error => error.code === 'unauthenticated' && error.message === 'Authentication required');
        await assert.rejects(handlers[name].run({ deviceToken: 'foreign-token' }, { auth: { uid: 'A' } }),
            error => error.code === 'permission-denied' && error.message === 'Device is not registered to this account');
        await assert.rejects(handlers[name].run({ deviceToken: 'fixture-token', cursor: 'foreign/path' }, { auth: { uid: 'A' } }),
            error => error.code === 'invalid-argument' && error.message === 'Invalid restore cursor');
    }
});
test('restore adapter rethrows infrastructure errors so retry/failure behavior is unchanged', async t => {
    const failure = Object.assign(new Error('temporary outage'), { code: 'unavailable' });
    t.mock.method(db, 'collection', () => ({ doc: () => ({ get: async () => { throw failure; } }) }));
    await assert.rejects(handlers.restoreScheduleState.run({ deviceToken: 'fixture-token' }, { auth: { uid: 'A' } }),
        error => error === failure);
});
test('transient FCM failure retains the retry error code and does not acknowledge an undelivered event', async t => {
    t.mock.method(db, 'collection', name => {
        assert.equal(name, 'users');
        return { doc: () => ({ get: async () => ({ data: () => ({ fcmTokens: ['fixture-token'] }) }) }) };
    });
    t.mock.getter(admin, 'messaging', () => () => ({ send: async () => {
        throw Object.assign(new Error('temporary FCM outage'), { code: 'messaging/server-unavailable' });
    } }));
    const updates = [];
    const ref = { get: async () => ({ exists: true, id: 'fixture-event',
        data: () => ({ userId: 'A', transactionId: 'p1', revision: 1, sent: false }) }),
    update: async value => { updates.push(value); } };
    await assert.rejects(handlers.onNotificationEvent.run({ ref }),
        error => error instanceof ScheduleError && error.code === ScheduleErrorCode.FCM_TRANSIENT_FAILURE);
    assert.deepEqual(updates, []);
});
test('registered callable HTTP wrappers still reject missing App Check before invoking their handlers', async () => {
    for (const name of ['checkRegisteredAccount', 'checkPasswordResetIdentity', 'restoreScheduleState']) {
        const req = { method: 'POST', headers: { 'content-type': 'application/json' }, body: { data: {} },
            header: name => req.headers[name.toLowerCase()] };
        const res = new EventEmitter();
        const headers = new Map();
        res.getHeader = name => headers.get(name);
        res.setHeader = (name, value) => headers.set(name, value);
        res.status = code => { res.statusCode = code; return res; };
        res.send = body => { res.body = body; res.emit('finish'); return res; };
        await handlers[name](req, res);
        assert.equal(res.statusCode, 401);
        assert.deepEqual(res.body, { error: { status: 'UNAUTHENTICATED', message: 'Unauthenticated' } });
    }
});
test('v1 trigger identities, callable regions and failure policies remain unchanged', () => {
    const paths = {
        sendScheduledNotification: ['scheduled_transactions/{transactionId}', 'providers/cloud.firestore/eventTypes/document.create'],
        onScheduledTransactionChanged: ['scheduled_transactions/{transactionId}', 'providers/cloud.firestore/eventTypes/document.update'],
        onScheduleCommand: ['schedule_commands/{commandId}', 'providers/cloud.firestore/eventTypes/document.create'],
        onNotificationEvent: ['notification_events/{eventId}', 'providers/cloud.firestore/eventTypes/document.create'],
        onScheduledTransactionDelete: ['scheduled_transactions/{transactionId}', 'providers/cloud.firestore/eventTypes/document.delete']
    };
    for (const [name, [path, type]] of Object.entries(paths)) {
        const trigger = handlers[name].__trigger;
        assert.equal(trigger.eventTrigger.resource, 'projects/demo-financeai/databases/(default)/documents/' + path);
        assert.equal(trigger.eventTrigger.eventType, type);
        assert.deepEqual(trigger.failurePolicy, { retry: {} });
    }
    for (const name of ['checkRegisteredAccount', 'checkPasswordResetIdentity', 'restoreScheduleState']) {
        assert.equal(handlers[name].__endpoint.platform, 'gcfv1');
        assert.deepEqual(handlers[name].__trigger.regions, ['us-central1']);
    }
});
