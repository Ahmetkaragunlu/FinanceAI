'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { reconcilePlan, completedPlanNeedsChoice } = require('../../src/schedule/plan-reconciliation');
const before = { amountMinor: 100, currencyCode: 'TRY', scheduledDate: 1000, note: 'before' };
test('disjoint local/remote plan edits merge before atomic completion', () => {
    const result = reconcilePlan(before, { ...before, note: 'local' }, { ...before, amountMinor: 200 });
    assert.equal(result.note, 'local');
    assert.equal(result.amountMinor, 200);
});
test('overlapping unequal edits require an explicit choice', () => {
    assert.equal(reconcilePlan(before, { ...before, amountMinor: 150 }, { ...before, amountMinor: 200 }), null);
});
test('identical concurrent values do not produce a false conflict', () => {
    assert.equal(reconcilePlan(before, { ...before, amountMinor: 200 }, { ...before, amountMinor: 200 }).amountMinor, 200);
});
test('completion keeps the explicit financial/photo allowlist and excludes date and provenance injection', () => {
    const local = { ...before, type: 'EXPENSE', category: 'FOOD', amount: 1,
        locationFull: 'Full', locationShort: 'Short', latitude: 10, longitude: 20,
        photoStorageUrl: 'owned-photo', photoRemoved: false, photoVersion: 'version', photoIntent: 'intent',
        date: 9999, completedFromScheduledId: 'foreign', completedFrom: 'foreign',
        userId: 'B', revision: 999, mutationId: 'foreign', deleted: true,
        notificationSent: true, expirationNotificationSent: true };
    assert.deepEqual(reconcilePlan(null, local, null), {
        amountMinor: 100, currencyCode: 'TRY', amount: 1, type: 'EXPENSE', category: 'FOOD', note: 'before',
        scheduledDate: 1000, locationFull: 'Full', locationShort: 'Short', latitude: 10, longitude: 20,
        photoStorageUrl: 'owned-photo', photoRemoved: false, photoVersion: 'version', photoIntent: 'intent'
    });
});
test('completed-plan choice ignores scheduled date/provenance while retaining editable financial/photo differences', () => {
    const base = { ...before, type: 'EXPENSE', photoIntent: 'first' };
    const financial = { ...base, transaction: 'EXPENSE', date: 3000, completedFromScheduledId: 'p1' };
    delete financial.type;
    assert.equal(completedPlanNeedsChoice(base, { ...base, scheduledDate: 2000,
        date: 9000, completedFromScheduledId: 'other' }, financial), false);
    assert.equal(completedPlanNeedsChoice(base, { ...base, photoIntent: 'second' }, financial), true);
    assert.equal(completedPlanNeedsChoice(base, { ...base, amountMinor: 200 }, financial), true);
});
test('completed-plan note and transaction-kind normalization do not create false conflicts', () => {
    assert.equal(completedPlanNeedsChoice({ type: 'EXPENSE', note: null }, { type: 'EXPENSE', note: '' },
        { transaction: 'EXPENSE', note: '' }), false);
    assert.equal(completedPlanNeedsChoice({ type: 'EXPENSE' }, { type: 'INCOME' },
        { transaction: 'EXPENSE' }), true);
});
