'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const fixture = JSON.parse(fs.readFileSync(path.resolve(__dirname,
    '../../../app/src/test/resources/contracts/schedule-contract.json'), 'utf8'));
const { ScheduleCollections: C, PlanFields, ScheduleCommandType, ScheduleStatus,
    CommandOutcome, completedTransactionId } = require('../../src/schedule/schedule-contract');
const { FinancialFields: F } = require('../../src/financial-contract');
const { initialState, dayTimes, tick, transition, HOUR, RETENTION } = require('../../src/schedule/reminder-policy');
const { scheduleService } = require('../../src/schedule/schedule-service');
const { firestoreMemory } = require('../support/firestore-memory');

test('shared collection money photo and outcome wire values match the independent Kotlin fixture', () => {
    assert.deepEqual(fixture.collections, { plans: C.PLANS, states: C.STATES, commands: C.COMMANDS, financial: C.FINANCIAL });
    assert.equal(fixture.fields.amountMinor, F.AMOUNT_MINOR);
    assert.equal(fixture.fields.currencyCode, F.CURRENCY_CODE);
    assert.equal(fixture.fields.scheduledDate, PlanFields.SCHEDULED_DATE);
    assert.equal(fixture.fields.type, PlanFields.TYPE);
    assert.deepEqual(fixture.photoMetadata, [F.PHOTO_STORAGE_URL, F.PHOTO_REMOVED, F.PHOTO_VERSION, F.PHOTO_INTENT]);
    assert.deepEqual(fixture.commandTypes, Object.values(ScheduleCommandType));
    assert.deepEqual(fixture.statuses, Object.values(ScheduleStatus));
    assert.deepEqual(fixture.outcomes.map(value => value.wire), Object.values(CommandOutcome));
    assert.equal(completedTransactionId(fixture.identity.plan), fixture.identity.completed);
});

test('calendar slots DST and consumed masks match server timing while display authority stays on devices', () => {
    for (const day of fixture.days) {
        const plan = { userId: 'fixture-account', scheduledDate: Date.parse(day.date) };
        assert.deepEqual(dayTimes(plan.scheduledDate, day.zone), { morning: Date.parse(day.morning),
            evening: Date.parse(day.evening), end: Date.parse(day.end) });
        for (const check of fixture.checks) {
            const state = { ...initialState(plan, day.zone), automaticSlots: check.progressSlots,
                dueAt: Date.parse(day[check.dueBefore]) };
            const next = tick(state, Date.parse(day[check.time]) + check.offsetMillis, day.zone);
            if (check.serverSlots == null) assert.equal(next, null);
            else {
                assert.equal(next.automaticSlots, check.serverSlots);
                assert.equal(next.dueAt, check.serverDue == null ? null : Date.parse(day[check.serverDue]));
                assert.equal(next.expirationAcceptedAt, null);
                assert.equal(next.deleteAt, null);
            }
        }
    }
});

test('offline snooze and first accepted expiration keep the shared durations and one retention deadline', () => {
    const day = fixture.days[0], plan = { userId: 'fixture-account', scheduledDate: Date.parse(day.date) };
    assert.equal(HOUR, fixture.snoozeMillis);
    assert.equal(RETENTION, fixture.retentionMillis);
    const snooze = transition(plan, initialState(plan, day.zone), { userId: plan.userId,
        scheduledDate: plan.scheduledDate, type: 'snooze', requestedAt: Date.parse(fixture.snooze.requested) },
    Date.parse(fixture.snooze.reconnected), day.zone);
    assert.equal(snooze.state.snoozeAt, Date.parse(fixture.snooze.due));
    const accepted = Date.parse(fixture.expiration.accepted), deadline = Date.parse(fixture.expiration.deadline);
    const first = transition(plan, initialState(plan, day.zone), { userId: plan.userId, scheduledDate: plan.scheduledDate,
        type: 'expiration_shown', requestedAt: Date.parse(fixture.expiration.shown) }, accepted, day.zone);
    assert.equal(first.state.deleteAt, deadline);
    const repeated = transition(plan, first.state, { userId: plan.userId, scheduledDate: plan.scheduledDate,
        type: 'expiration_shown', requestedAt: accepted }, accepted + HOUR, day.zone);
    assert.equal(repeated.state.deleteAt, deadline);
    assert.equal(repeated.outcome, 'already_applied');
    assert.equal(tick(first.state, deadline - 1, day.zone), null);
    assert.equal(tick(first.state, deadline, day.zone).status, 'deleted');
});

test('commands constructed with shared field names produce the expected persisted receipt and financial identity', async () => {
    const f = fixture.fields, c = fixture.collections, date = Date.parse(fixture.days[0].date);
    const plan = { [f.userId]: 'fixture-account', [f.scheduledDate]: date, [f.amountMinor]: 12550,
        [f.currencyCode]: 'USD', [f.type]: 'EXPENSE', category: 'FOOD', note: 'fixture' };
    const command = { [f.userId]: plan[f.userId], [f.remoteId]: fixture.identity.plan,
        [f.type]: 'complete', [f.scheduledDate]: date, [f.requestedAt]: date, processed: false, plan };
    const db = firestoreMemory({ 'users/fixture-account': { currencyCode: 'USD', timeZoneId: fixture.days[0].zone },
        [c.plans + '/' + fixture.identity.plan]: plan, [c.commands + '/fixture-command']: command });
    await scheduleService(db, () => date).processCommand('fixture-command');
    assert.equal(db.values.get(c.commands + '/fixture-command')[f.outcome], 'applied');
    assert.equal(db.values.get(c.financial + '/' + fixture.identity.completed)[f.amountMinor], 12550);
});
