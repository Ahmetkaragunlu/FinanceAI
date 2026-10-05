'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { scheduleRestore } = require('../src/schedule-restore');
const { ScheduleError, ScheduleErrorCode, scheduleRestoreFailure } = require('../src/schedule-errors');

function fixture() {
    const calls = [];
    const refreshed = [];
    let rows = [];
    const query = {
        where: (...args) => { calls.push(['where', ...args]); return query; },
        orderBy: field => { calls.push(['orderBy', field]); return query; },
        limit: count => { calls.push(['limit', count]); return query; },
        startAfter: cursor => { calls.push(['startAfter', cursor]); return query; },
        get: async () => ({ size: rows.length, docs: rows })
    };
    const db = { collection: name => {
        if (name === 'users') return { doc: uid => ({ get: async () => {
            calls.push(['user', uid]);
            return { data: () => ({ fcmTokens: ['fixture-device'] }) };
        } }) };
        assert.equal(name, 'scheduled_transactions');
        return query;
    } };
    return { calls, refreshed, rows: values => { rows = values; },
        restore: scheduleRestore(db, { refresh: async id => { refreshed.push(id); } }, 'document-id') };
}
const context = { auth: { uid: 'A' } };

test('restore rejects unauthenticated calls before reading any account', async () => {
    const { restore, calls } = fixture();
    await assert.rejects(restore({ deviceToken: 'fixture-device' }, {}),
        error => error instanceof ScheduleError && error.code === ScheduleErrorCode.AUTHENTICATION_REQUIRED);
    assert.deepEqual(calls, []);
});
test('restore rejects unregistered device tokens without querying plans', async () => {
    for (const data of [{ deviceToken: 'foreign-device' }, {}, null]) {
        const { restore, calls } = fixture();
        await assert.rejects(restore(data, context),
            error => error instanceof ScheduleError && error.code === ScheduleErrorCode.DEVICE_NOT_REGISTERED);
        assert.deepEqual(calls, [['user', 'A']]);
    }
});
test('restore validates cursor without allowing cross-path or invalid cursor values', async () => {
    for (const cursor of ['foreign/path', 'x'.repeat(513), 123]) {
        const { restore, calls } = fixture();
        await assert.rejects(restore({ deviceToken: 'fixture-device', cursor }, context),
            error => error instanceof ScheduleError && error.code === ScheduleErrorCode.INVALID_RESTORE_CURSOR);
        assert.deepEqual(calls, [['user', 'A']]);
    }
});
test('restore keeps owned bounded pagination and skips deleted plans without inventing a reminder', async () => {
    const { restore, calls, refreshed, rows } = fixture();
    rows(Array.from({ length: 100 }, (_, i) => ({ id: 'p' + String(i).padStart(3, '0'),
        data: () => ({ deleted: i === 0 }) })));
    const result = await restore({ deviceToken: 'fixture-device', cursor: 'previous-page' }, context);
    assert.deepEqual(result, { success: true, count: 100, nextCursor: 'p099' });
    assert.deepEqual(calls, [['user', 'A'], ['where', 'userId', '==', 'A'],
        ['orderBy', 'document-id'], ['limit', 100], ['startAfter', 'previous-page']]);
    assert.equal(refreshed.length, 99);
    assert.equal(refreshed.includes('p000'), false);
});
test('a shorter restored page has no next cursor and infrastructure failures retain identity', async () => {
    const { restore, rows } = fixture();
    rows([{ id: 'p1', data: () => ({ deleted: false }) }]);
    assert.deepEqual(await restore({ deviceToken: 'fixture-device' }, context),
        { success: true, count: 1, nextCursor: null });
    const failure = Object.assign(new Error('temporary outage'), { code: 'unavailable' });
    const failedDb = { collection: () => ({ doc: () => ({ get: async () => { throw failure; } }) }) };
    await assert.rejects(scheduleRestore(failedDb, {}, 'document-id')({ deviceToken: 'fixture-device' }, context),
        error => error === failure);
    assert.equal(scheduleRestoreFailure(failure), null);
});
test('restore maps only its typed expected errors, never diagnostic message text', () => {
    for (const [code, result] of [
        [ScheduleErrorCode.AUTHENTICATION_REQUIRED, { code: 'unauthenticated', message: 'Authentication required' }],
        [ScheduleErrorCode.DEVICE_NOT_REGISTERED, { code: 'permission-denied', message: 'Device is not registered to this account' }],
        [ScheduleErrorCode.INVALID_RESTORE_CURSOR, { code: 'invalid-argument', message: 'Invalid restore cursor' }]
    ]) {
        const error = new ScheduleError(code);
        error.message = 'Different diagnostics';
        assert.deepEqual(scheduleRestoreFailure(error), result);
    }
    assert.equal(scheduleRestoreFailure(new Error('INVALID_RESTORE_CURSOR')), null);
    assert.equal(scheduleRestoreFailure(new ScheduleError(ScheduleErrorCode.FCM_TRANSIENT_FAILURE)), null);
});
