'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { DateTime } = require('luxon');
const { HOUR, dayTimes, initialState, transition, tick } = require('../../src/schedule/reminder-policy');
const { ScheduleError, ScheduleErrorCode } = require('../../src/schedule/schedule-errors');
const zone = 'Europe/Istanbul';
const at = (hour, day = 5) => DateTime.fromObject({ year: 2026, month: 10, day, hour }, { zone }).toMillis();
const plan = { userId: 'A', scheduledDate: at(0), amountMinor: 12550, currencyCode: 'TRY' };
const command = (type, requestedAt = at(10)) => ({ userId: 'A', scheduledDate: plan.scheduledDate, type, requestedAt });

test('09 and 18 use account zone rather than the server zone', () => {
    const times = dayTimes(plan.scheduledDate, zone);
    assert.equal(times.morning, at(9));
    assert.equal(times.evening, at(18));
    assert.equal(times.end, at(0, 6));
});
test('DST day ends at the next calendar midnight, not a fixed 24 hours', () => {
    const date = DateTime.fromISO('2026-03-29T00:00', { zone: 'Europe/Berlin' }).toMillis();
    assert.equal(dayTimes(date, 'Europe/Berlin').end - date, 23 * HOUR);
});
test('before due time no automatic event is produced', () => {
    assert.equal(tick(initialState(plan, zone), at(8), zone), null);
});
test('a late wake consumes missed automatic slots without a burst', () => {
    const state = tick(initialState(plan, zone), at(19), zone);
    assert.equal(state.automaticSlots, 3);
    assert.equal(state.dueAt, at(0, 6));
});
test('snooze is one hour from the explicit action', () => {
    const next = transition(plan, initialState(plan, zone), command('snooze'), at(10), zone);
    assert.equal(next.state.snoozeAt, at(11));
    assert.equal(next.state.dueAt, at(11));
});
test('offline snooze is not restarted for an extra hour after reconnect', () => {
    const next = transition(plan, initialState(plan, zone), command('snooze'), at(12), zone);
    assert.equal(next.state.snoozeAt, at(11));
});
test('a stale action cannot shorten a newer account-wide snooze', () => {
    const first = transition(plan, initialState(plan, zone), command('snooze', at(11)), at(11), zone).state;
    const next = transition(plan, first, command('snooze', at(10)), at(12), zone);
    assert.equal(next.outcome, 'stale');
    assert.equal(next.state.snoozeAt, at(12));
});
test('day end cannot manufacture a successful display receipt', () => {
    const state = tick(initialState(plan, zone), at(0, 6), zone);
    assert.equal(state.deleteAt, null);
    assert.equal(state.expirationAcceptedAt, null);
    assert.equal(state.dueAt, null);
});
test('first accepted expiration display starts one shared 24-hour retention', () => {
    const accepted = at(12, 6);
    const first = transition(plan, initialState(plan, zone), command('expiration_shown', at(0, 6)), accepted, zone).state;
    const second = transition(plan, first, command('expiration_shown'), at(14, 6), zone);
    assert.equal(first.deleteAt, accepted + 24 * HOUR);
    assert.equal(second.state.deleteAt, first.deleteAt);
    assert.equal(second.outcome, 'already_applied');
});
test('terminal plans cannot be resurrected by late snooze or display acknowledgements', () => {
    for (const status of ['completed', 'deleted']) {
        const state = { ...initialState(plan, zone), status };
        assert.equal(transition(plan, state, command('snooze'), at(10), zone).outcome, 'terminal');
        assert.equal(transition(plan, state, command('expiration_shown'), at(0, 6), zone).outcome, 'terminal');
    }
});
test('retention deletion only follows an accepted receipt and the deadline', () => {
    const state = transition(plan, initialState(plan, zone), command('expiration_shown'), at(0, 6), zone).state;
    assert.equal(tick(state, state.deleteAt - 1, zone), null);
    assert.equal(tick(state, state.deleteAt, zone).status, 'deleted');
});
test('wrong owner and invalid zone are rejected', () => {
    assert.throws(() => transition(plan, initialState(plan, zone), { ...command('snooze'), userId: 'B' }, at(10), zone),
        error => error instanceof ScheduleError && error.code === ScheduleErrorCode.OWNER_MISMATCH);
    assert.throws(() => dayTimes(plan.scheduledDate, 'not/a-zone'),
        error => error instanceof ScheduleError && error.code === ScheduleErrorCode.INVALID_TIME_ZONE);
});
test('unsupported commands and invalid snooze times carry explicit schedule error codes', () => {
    for (const [request, code] of [[command('unknown'), ScheduleErrorCode.INVALID_COMMAND],
        [command('snooze', at(11)), ScheduleErrorCode.INVALID_REQUEST_TIME],
        [command('snooze', 'invalid'), ScheduleErrorCode.INVALID_REQUEST_TIME]]) {
        assert.throws(() => transition(plan, initialState(plan, zone), request, at(10), zone),
            error => error instanceof ScheduleError && error.code === code);
    }
});
