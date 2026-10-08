'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { scheduleService } = require('../../src/schedule/schedule-service');
const { firestoreMemory } = require('../support/firestore-memory');
const { dayTimes, initialState, HOUR } = require('../../src/schedule/reminder-policy');
const date = Date.parse('2026-10-05T00:00:00+03:00');
const plan = { userId: 'A', scheduledDate: date, currencyCode: 'TRY', amountMinor: 5025,
    amount: 50.25, type: 'EXPENSE', category: 'FOOD', note: 'Receipt', revision: 1, deleted: false };
const command = (type, requestedAt = date + 10 * HOUR) => ({ type, userId: 'A', transactionId: 'p1',
    scheduledDate: date, requestedAt, processed: false, plan });
function fixture(commands, extra = {}) {
    const db = firestoreMemory({ 'users/A': { currencyCode: 'TRY', timeZoneId: 'Europe/Istanbul' },
        'scheduled_transactions/p1': plan, ...commands, ...extra });
    return db;
}

test('two devices complete one plan with a single financial winner', async () => {
    const db = fixture({ 'schedule_commands/c1': command('complete'),
        'schedule_commands/c2': command('complete', date + 11 * HOUR) });
    const service = scheduleService(db, () => date + 12 * HOUR);
    await Promise.all([service.processCommand('c1'), service.processCommand('c2')]);
    assert.equal(db.writes.filter(path => path === 'transactions/completed_p1').length, 1);
    assert.equal(db.values.get('transactions/completed_p1').date, date + 10 * HOUR);
    assert.equal(db.values.get('scheduled_transactions/p1').deleted, true);
    assert.equal(db.values.get('schedule_states/p1').status, 'completed');
    assert.equal(db.values.get('schedule_commands/c2').outcome, 'already_applied');
});
test('retry after a successful atomic commit does not create another event or financial row', async () => {
    const db = fixture({ 'schedule_commands/c1': command('complete') });
    const service = scheduleService(db, () => date + 12 * HOUR);
    await service.processCommand('c1');
    const count = db.writes.length;
    await service.processCommand('c1');
    assert.equal(db.writes.length, count);
});
test('an offline-created plan can be completed without a separate remote create/delete race', async () => {
    const db = fixture({ 'schedule_commands/c1': command('complete') });
    db.values.delete('scheduled_transactions/p1');
    await scheduleService(db, () => date + 12 * HOUR).processCommand('c1');
    assert.equal(db.values.get('transactions/completed_p1').amountMinor, 5025);
    assert.equal(db.values.get('scheduled_transactions/p1').deleted, true);
});
test('another account cannot complete or snooze a plan', async () => {
    for (const type of ['complete', 'snooze']) {
        const db = fixture({ 'schedule_commands/c1': { ...command(type), userId: 'B' } },
            { 'users/B': { currencyCode: 'TRY', timeZoneId: 'Europe/Istanbul' } });
        await scheduleService(db, () => date + 12 * HOUR).processCommand('c1');
        assert.equal(db.values.get('schedule_commands/c1').outcome, 'forbidden');
        assert.equal(db.values.has('transactions/completed_p1'), false);
        assert.equal(db.values.get('scheduled_transactions/p1').deleted, false);
    }
});
test('late completion of a deleted plan does not resurrect it', async () => {
    const db = fixture({ 'schedule_commands/c1': command('complete') },
        { 'scheduled_transactions/p1': { ...plan, deleted: true } });
    await scheduleService(db, () => date + 12 * HOUR).processCommand('c1');
    assert.equal(db.values.get('schedule_commands/c1').outcome, 'terminal');
    assert.equal(db.values.has('transactions/completed_p1'), false);
});
test('two expiration acknowledgements retain the first server acceptance', async () => {
    const now = dayTimes(date, 'Europe/Istanbul').end + 12 * HOUR;
    const db = fixture({ 'schedule_commands/c1': command('expiration_shown'),
        'schedule_commands/c2': command('expiration_shown') });
    await scheduleService(db, () => now).processCommand('c1');
    await scheduleService(db, () => now + HOUR).processCommand('c2');
    assert.equal(db.values.get('schedule_states/p1').deleteAt, now + 24 * HOUR);
    assert.equal(db.values.get('schedule_commands/c2').outcome, 'already_applied');
});
test('expiration uses tombstones and preserves completed financial data', async () => {
    const now = date + 72 * HOUR;
    const state = { ...initialState(plan, 'Europe/Istanbul'), expirationAcceptedAt: now - 24 * HOUR,
        deleteAt: now, dueAt: now };
    const financial = { userId: 'A', amountMinor: 10000, currencyCode: 'TRY', transaction: 'INCOME', date };
    const db = fixture({}, { 'schedule_states/p1': state, 'transactions/completed_previous': financial });
    await scheduleService(db, () => now).advance('p1');
    assert.equal(db.values.get('scheduled_transactions/p1').deleted, true);
    assert.equal(db.values.get('schedule_states/p1').status, 'deleted');
    assert.deepEqual(db.values.get('transactions/completed_previous'), financial);
});
test('invalid money is a retained failure receipt, not an infinite trigger retry', async () => {
    const invalid = { ...plan, amountMinor: -1 };
    const db = fixture({ 'schedule_commands/c1': { ...command('complete'), plan: invalid, base: invalid } },
        { 'scheduled_transactions/p1': invalid });
    await scheduleService(db, () => date + 12 * HOUR).processCommand('c1');
    assert.equal(db.values.get('schedule_commands/c1').outcome, 'invalid_money');
    assert.equal(db.values.has('transactions/completed_p1'), false);
});
test('a competing plan edit is retained as a conflict without producing financial data', async () => {
    const local = { ...plan, amountMinor: 6000 };
    const db = fixture({ 'schedule_commands/c1': { ...command('complete'), plan: local, base: plan } },
        { 'scheduled_transactions/p1': { ...plan, amountMinor: 7000 } });
    await scheduleService(db, () => date + 12 * HOUR).processCommand('c1');
    assert.equal(db.values.get('schedule_commands/c1').outcome, 'conflict');
    assert.equal(db.values.has('transactions/completed_p1'), false);
    assert.equal(db.values.get('scheduled_transactions/p1').amountMinor, 7000);
});
test('a foreign existing completion is a permanent receipt, not an infinite trigger retry', async () => {
    const db = fixture({ 'schedule_commands/c1': command('complete') },
        { 'transactions/completed_p1': { userId: 'B', amountMinor: 999 } });
    await scheduleService(db, () => date + 12 * HOUR).processCommand('c1');
    assert.equal(db.values.get('schedule_commands/c1').outcome, 'forbidden');
    assert.equal(db.values.get('transactions/completed_p1').amountMinor, 999);
    assert.equal(db.values.get('scheduled_transactions/p1').deleted, false);
});
test('invalid account zone is retained as a failure receipt without committing money', async () => {
    const db = fixture({ 'schedule_commands/c1': command('complete') },
        { 'users/A': { currencyCode: 'TRY', timeZoneId: 'not/a-zone' } });
    await scheduleService(db, () => date + 12 * HOUR).processCommand('c1');
    assert.equal(db.values.get('schedule_commands/c1').processed, true);
    assert.equal(db.values.has('transactions/completed_p1'), false);
});
test('invalid due-state rows cannot permanently occupy the cron batch', async () => {
    const db = fixture({}, { 'schedule_states/p1': { ...initialState(plan, 'Europe/Istanbul'), scheduledDate: 'invalid', dueAt: date } });
    await scheduleService(db, () => date + 12 * HOUR).advance('p1');
    assert.equal(db.values.get('schedule_states/p1').dueAt, null);
});
test('an offline edited completion cannot silently overwrite a different completed winner', async () => {
    const financial = { ...plan, transaction: plan.type, date: date + 8 * HOUR, deleted: false };
    const db = fixture({ 'schedule_commands/c1': { ...command('complete'), base: plan, plan: { ...plan, amountMinor: 7500 } } },
        { 'transactions/completed_p1': financial, 'scheduled_transactions/p1': { ...plan, deleted: true, completedFrom: 'completed_p1' } });
    await scheduleService(db, () => date + 12 * HOUR).processCommand('c1');
    assert.equal(db.values.get('schedule_commands/c1').conflictTarget, 'transactions');
    assert.equal(db.values.get('transactions/completed_p1').amountMinor, plan.amountMinor);
    assert.equal(db.values.get('transactions/completed_p1').date, date + 8 * HOUR);
});
test('completion strips plan-only fields and owns the original date/provenance despite supplied extra fields', async () => {
    const request = { ...command('complete'), plan: { ...plan, scheduledDate: date,
        date: date + 99 * HOUR, completedFromScheduledId: 'foreign', userId: 'foreign',
        notificationSent: true, expirationNotificationSent: true } };
    const db = fixture({ 'schedule_commands/c1': request });
    await scheduleService(db, () => date + 12 * HOUR).processCommand('c1');
    const financial = db.values.get('transactions/completed_p1');
    assert.equal(financial.date, request.requestedAt);
    assert.equal(financial.completedFromScheduledId, 'p1');
    assert.equal(financial.userId, 'A');
    assert.equal(financial.transaction, 'EXPENSE');
    for (const field of ['scheduledDate', 'type', 'notificationSent', 'expirationNotificationSent'])
        assert.equal(Object.hasOwn(financial, field), false);
});
test('an unknown command remains an invalid retained receipt with no schedule or financial mutation', async () => {
    const db = fixture({ 'schedule_commands/c1': command('unknown') });
    await scheduleService(db, () => date + 12 * HOUR).processCommand('c1');
    assert.equal(db.values.get('schedule_commands/c1').outcome, 'invalid');
    assert.equal(db.values.get('schedule_commands/c1').processed, true);
    assert.deepEqual(db.values.get('scheduled_transactions/p1'), plan);
    assert.equal(db.values.has('transactions/completed_p1'), false);
    assert.equal(db.values.has('schedule_states/p1'), false);
});
