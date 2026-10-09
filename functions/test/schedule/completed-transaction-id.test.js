'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { completedTransactionId } = require('../../src/schedule/schedule-contract');

test('completion identities retain their persisted prefix and exact plan suffix', () => {
    for (const plan of ['p1', 'photo-plan', 'plan_with_underscores', 'completed_plan', '']) {
        assert.equal(completedTransactionId(plan), `completed_${plan}`);
    }
});
