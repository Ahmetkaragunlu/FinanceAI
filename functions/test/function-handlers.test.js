'use strict';

const { test, after, mock } = require('node:test');
const assert = require('node:assert/strict');
const { EventEmitter } = require('node:events');
const admin = require('firebase-admin');
const handlerNow = Date.parse('2026-10-09T00:00:00Z');
mock.method(Date, 'now', () => handlerNow);
const { firestoreMemory } = require('./support/firestore-memory');
const { ScheduleError, ScheduleErrorCode } = require('../src/schedule/schedule-errors');

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

function notificationEventFixture(t, tokens, deliver = async () => 'message-id') {
    const sent = [];
    const event = { userId: 'A', transactionId: 'p1', revision: 3, sent: false, delivered: [] };
    const user = { fcmTokens: [...tokens] };
    t.mock.method(admin.firestore.FieldValue, 'arrayUnion', (...values) => ({ add: values }));
    t.mock.method(admin.firestore.FieldValue, 'arrayRemove', (...values) => ({ remove: values }));
    t.mock.method(db, 'collection', name => {
        assert.equal(name, 'users', 'Event delivery must not write financial records');
        return { doc: uid => {
            assert.equal(uid, 'A');
            return {
                get: async () => ({ data: () => structuredClone(user) }),
                update: async patch => {
                    user.fcmTokens = user.fcmTokens.filter(token => !patch.fcmTokens.remove.includes(token));
                }
            };
        } };
    });
    t.mock.getter(admin, 'messaging', () => () => ({ send: async message => {
        sent.push(message);
        return deliver(message.token);
    } }));
    const ref = {
        get: async () => ({ exists: true, id: 'p1_3', data: () => structuredClone(event) }),
        update: async patch => {
            if (patch.delivered) event.delivered = [...new Set([...event.delivered, ...patch.delivered.add])];
            if (Object.hasOwn(patch, 'sent')) event.sent = patch.sent;
        }
    };
    return { event, user, sent, snapshot: { ref } };
}

test('one account event reaches every registered device once and ignores duplicate tokens and trigger retries', async t => {
    const fixture = notificationEventFixture(t, ['first-device', 'second-device', 'first-device']);
    await handlers.onNotificationEvent.run(fixture.snapshot);
    await handlers.onNotificationEvent.run(fixture.snapshot);
    assert.deepEqual(fixture.sent.map(message => message.token), ['first-device', 'second-device']);
    for (const message of fixture.sent) assert.deepEqual(message.data, {
        type: 'SCHEDULE_STATE_CHANGED', userId: 'A', transactionId: 'p1', eventId: 'p1_3', revision: '3'
    });
    assert.equal(fixture.event.sent, true);
    assert.equal(fixture.event.delivered.length, 2);
});

test('partial fan-out retries only the device that failed and retains successful delivery receipts', async t => {
    let unavailable = true;
    const fixture = notificationEventFixture(t, ['first-device', 'second-device'], async token => {
        if (token === 'second-device' && unavailable)
            throw Object.assign(new Error('temporary outage'), { code: 'messaging/server-unavailable' });
        return 'message-id';
    });
    await assert.rejects(handlers.onNotificationEvent.run(fixture.snapshot),
        error => error instanceof ScheduleError && error.code === ScheduleErrorCode.FCM_TRANSIENT_FAILURE);
    assert.equal(fixture.event.sent, false);
    assert.equal(fixture.event.delivered.length, 1);
    unavailable = false;
    await handlers.onNotificationEvent.run(fixture.snapshot);
    assert.deepEqual(fixture.sent.map(message => message.token), ['first-device', 'second-device', 'second-device']);
    assert.equal(fixture.event.sent, true);
    assert.equal(fixture.event.delivered.length, 2);
});

test('an unregistered token is removed while valid devices complete without a transient retry', async t => {
    const fixture = notificationEventFixture(t, ['dead-device', 'valid-device'], async token => {
        if (token === 'dead-device')
            throw Object.assign(new Error('unregistered device'), { code: 'messaging/registration-token-not-registered' });
        return 'message-id';
    });
    await handlers.onNotificationEvent.run(fixture.snapshot);
    assert.deepEqual(fixture.user.fcmTokens, ['valid-device']);
    assert.equal(fixture.event.sent, true);
    assert.equal(fixture.event.delivered.length, 1);
    assert.deepEqual(fixture.sent.map(message => message.token), ['dead-device', 'valid-device']);
});

const handlerPlan = { userId: 'A', scheduledDate: handlerNow, amountMinor: 1250, currencyCode: 'USD',
    type: 'EXPENSE', category: 'FOOD', note: 'fixture', revision: 2, deleted: false };
function handlerMemory(t, seed = {}) {
    const memory = firestoreMemory({ 'users/A': { currencyCode: 'USD', timeZoneId: 'UTC', fcmTokens: [] }, ...seed });
    t.mock.method(db, 'collection', memory.collection);
    t.mock.method(db, 'runTransaction', memory.runTransaction);
    return memory;
}

test('invalid event bodies are acknowledged as invalid without accessing users or sending FCM', async () => {
    for (const event of [{ userId: 1, transactionId: 'p1', revision: 1 },
        { userId: 'A', transactionId: null, revision: 1 }, { userId: 'A', transactionId: 'p1', revision: 1.5 }]) {
        const updates = [];
        const ref = { get: async () => ({ exists: true, data: () => event }), update: async patch => updates.push(patch) };
        await handlers.onNotificationEvent.run({ ref });
        assert.deepEqual(updates, [{ sent: true, failure: 'invalid_event' }]);
    }
});

test('missing event snapshots are safe no-ops', async () => {
    await handlers.onNotificationEvent.run({ ref: {
        get: async () => ({ exists: false }), update: async () => assert.fail('Missing event must not be updated')
    } });
});

test('plan creation and command exports dispatch the actual refresh and atomic completion bodies', async t => {
    const memory = handlerMemory(t, { 'scheduled_transactions/p1': handlerPlan,
        'schedule_commands/c1': { userId: 'A', transactionId: 'p1', type: 'complete', scheduledDate: handlerNow,
            requestedAt: handlerNow, processed: false, plan: handlerPlan } });
    await handlers.sendScheduledNotification.run({}, { params: { transactionId: 'p1' } });
    assert.equal(memory.values.get('schedule_states/p1').revision, 1);
    await handlers.onScheduleCommand.run({}, { params: { commandId: 'c1' } });
    assert.equal(memory.values.get('schedule_commands/c1').outcome, 'applied');
    assert.equal(memory.values.get('transactions/completed_p1').mutationId, 'c1');
    assert.equal(memory.values.get('schedule_states/p1').status, 'completed');
});

test('plan update export refreshes only the existing deleted or scheduled-date transitions', async t => {
    const memory = handlerMemory(t);
    const context = { params: { transactionId: 'p1' } };
    for (const patch of [{ note: 'changed' }, { amountMinor: 5000 },
        { scheduledDate: handlerNow + 86400000 }, { deleted: true }]) {
        const after = { ...handlerPlan, ...patch };
        memory.values.set('scheduled_transactions/p1', after);
        memory.values.delete('schedule_states/p1');
        memory.writes.length = 0;
        await handlers.onScheduledTransactionChanged.run({ before: { data: () => handlerPlan },
            after: { data: () => after } }, context);
        if (Object.hasOwn(patch, 'note') || Object.hasOwn(patch, 'amountMinor')) assert.deepEqual(memory.writes, []);
        else {
            assert.equal(memory.values.get('schedule_states/p1').scheduledDate, after.scheduledDate);
            assert.equal(memory.values.get('schedule_states/p1').status, after.deleted ? 'deleted' : 'active');
            assert.equal(memory.writes.filter(path => path.startsWith('notification_events/')).length, 1);
        }
    }
});

test('delete export creates a monotonic tombstone and matching event without deleting completed money', async t => {
    const financial = { userId: 'A', amountMinor: 1250, currencyCode: 'USD', date: handlerNow };
    const memory = handlerMemory(t, { 'transactions/completed_p1': financial });
    for (const previous of [null, { userId: 'A', scheduledDate: handlerNow, status: 'active', revision: 7,
        snoozeAt: handlerNow + 3600000, dueAt: handlerNow + 3600000 }]) {
        if (previous) memory.values.set('schedule_states/p1', previous);
        else memory.values.delete('schedule_states/p1');
        await handlers.onScheduledTransactionDelete.run({ data: () => handlerPlan }, { params: { transactionId: 'p1' } });
        const revision = previous ? 8 : 1;
        assert.deepEqual(memory.values.get('schedule_states/p1'), { ...(previous || {}), userId: 'A',
            scheduledDate: handlerNow, status: 'deleted', dueAt: null, snoozeAt: null, revision });
        assert.deepEqual(memory.values.get('notification_events/p1_' + revision), {
            userId: 'A', transactionId: 'p1', revision, createdAt: handlerNow, sent: false
        });
        assert.deepEqual(memory.values.get('transactions/completed_p1'), financial);
    }
});

test('cron processes bounded due and unsent batches and leaves the next page and future rows untouched', async t => {
    const seed = {};
    for (let i = 0; i < 101; i++) {
        const id = 'p' + String(i).padStart(3, '0');
        seed['scheduled_transactions/' + id] = handlerPlan;
        seed['schedule_states/' + id] = { userId: 'A', scheduledDate: handlerNow, status: 'active',
            automaticSlots: 0, revision: 0, dueAt: handlerNow - 1, snoozeAt: null, deleteAt: null };
    }
    seed['schedule_states/future'] = { dueAt: handlerNow + 3600000 };
    const memory = handlerMemory(t, seed);
    await handlers.checkExpiredReminders.run({});
    assert.deepEqual(memory.queries.map(({ name, maximum, filters }) => ({ name, maximum, filters })), [
        { name: 'schedule_states', maximum: 100, filters: [['dueAt', '<=', handlerNow]] },
        { name: 'notification_events', maximum: 100, filters: [['sent', '==', false]] }
    ]);
    assert.equal(memory.values.get('schedule_states/p100').revision, 0);
    assert.equal(memory.values.get('schedule_states/future').dueAt, handlerNow + 3600000);
    assert.equal([...memory.values.entries()].filter(([path, value]) => path.startsWith('notification_events/') && value.sent).length, 100);
    assert.equal([...memory.values.keys()].some(path => path.startsWith('transactions/')), false);
});

test('cron retains a failed fan-out event for retry and later acknowledges the same event', async t => {
    const memory = handlerMemory(t, { 'users/A': { timeZoneId: 'UTC', fcmTokens: ['synthetic-token'] },
        'notification_events/e1': { userId: 'A', transactionId: 'p1', revision: 1, sent: false } });
    let unavailable = true, calls = 0;
    t.mock.method(admin.firestore.FieldValue, 'arrayUnion', (...values) => values);
    t.mock.getter(admin, 'messaging', () => () => ({ send: async () => {
        calls++;
        if (unavailable) throw Object.assign(new Error('temporary'), { code: 'messaging/server-unavailable' });
        return 'message';
    } }));
    await assert.rejects(handlers.checkExpiredReminders.run({}), error => error.code === ScheduleErrorCode.FCM_TRANSIENT_FAILURE);
    assert.equal(memory.values.get('notification_events/e1').sent, false);
    unavailable = false;
    await handlers.checkExpiredReminders.run({});
    assert.equal(calls, 2);
    assert.equal(memory.values.get('notification_events/e1').sent, true);
    assert.equal(memory.values.get('notification_events/e1').delivered.length, 1);
});

test('both restore callable exports page only the authenticated account and skip deleted plans', async t => {
    const memory = handlerMemory(t, {
        'users/A': { currencyCode: 'USD', timeZoneId: 'UTC', fcmTokens: ['synthetic-token'] },
        'scheduled_transactions/a-plan': handlerPlan,
        'scheduled_transactions/b-deleted': { ...handlerPlan, deleted: true },
        'scheduled_transactions/c-foreign': { ...handlerPlan, userId: 'B' }
    });
    for (const name of ['sendPendingNotifications', 'restoreScheduleState']) {
        assert.deepEqual(await handlers[name].run({ deviceToken: 'synthetic-token' }, { auth: { uid: 'A' } }),
            { success: true, count: 2, nextCursor: null });
        assert.deepEqual(await handlers[name].run({ deviceToken: 'synthetic-token', cursor: 'a-plan' }, { auth: { uid: 'A' } }),
            { success: true, count: 1, nextCursor: null });
    }
    assert.equal(memory.values.has('schedule_states/a-plan'), true);
    assert.equal(memory.values.has('schedule_states/b-deleted'), false);
    assert.equal(memory.values.has('schedule_states/c-foreign'), false);
});

test('legacy manual adapter refreshes only a live owned plan and consumes its old trigger', async t => {
    const memory = handlerMemory(t);
    for (const kind of ['owned', 'foreign', 'deleted', 'missing']) {
        memory.values.delete('scheduled_transactions/p1');
        if (kind !== 'missing') memory.values.set('scheduled_transactions/p1', { ...handlerPlan, deleted: kind === 'deleted' });
        memory.values.delete('schedule_states/p1');
        let deleted = 0;
        const snap = { data: () => ({ transactionId: 'p1', userId: kind === 'foreign' ? 'B' : 'A' }),
            ref: { delete: async () => { deleted++; } } };
        await handlers.manualSendToAllDevices.run(snap);
        assert.equal(deleted, 1);
        assert.equal(memory.values.has('schedule_states/p1'), kind === 'owned');
    }
});

test('legacy reminder adapter creates one idempotent snooze command and dismiss adapter only consumes its trigger', async t => {
    const memory = handlerMemory(t, { 'scheduled_transactions/p1': handlerPlan });
    let deleted = 0;
    const snap = { id: 'old-reminder', data: () => ({ transactionId: 'p1', userId: 'A' }),
        ref: { delete: async () => { deleted++; } } };
    await handlers.scheduleReminderTask.run(snap);
    const command = structuredClone(memory.values.get('schedule_commands/legacy_old-reminder'));
    assert.equal(command.type, 'snooze');
    assert.equal(command.outcome, 'applied');
    assert.equal(command.processed, true);
    await handlers.scheduleReminderTask.run(snap);
    assert.deepEqual(memory.values.get('schedule_commands/legacy_old-reminder'), command);
    assert.equal([...memory.values.keys()].filter(path => path.startsWith('schedule_commands/')).length, 1);
    assert.equal(memory.values.get('schedule_states/p1').snoozeAt, handlerNow + 3600000);
    await handlers.onNotificationDismissed.run(snap);
    assert.equal(deleted, 3);
    assert.equal([...memory.values.keys()].some(path => path.startsWith('transactions/')), false);
});

test('legacy reminder adapter consumes missing deleted or foreign triggers without creating commands', async t => {
    const memory = handlerMemory(t);
    for (const kind of ['missing', 'deleted', 'foreign']) {
        memory.values.delete('scheduled_transactions/p1');
        if (kind !== 'missing') memory.values.set('scheduled_transactions/p1', { ...handlerPlan, deleted: kind === 'deleted' });
        let deleted = 0;
        await handlers.scheduleReminderTask.run({ id: 'blocked-' + kind,
            data: () => ({ transactionId: 'p1', userId: kind === 'foreign' ? 'B' : 'A' }),
            ref: { delete: async () => { deleted++; } } });
        assert.equal(deleted, 1);
        assert.equal([...memory.values.keys()].some(path => path.startsWith('schedule_commands/')), false);
        assert.equal(memory.values.has('schedule_states/p1'), false);
    }
});
