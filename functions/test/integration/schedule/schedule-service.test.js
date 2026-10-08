'use strict';

const { before, beforeEach, after, test } = require('node:test');
const assert = require('node:assert/strict');
const { initializeTestEnvironment } = require('@firebase/rules-unit-testing');
const { scheduleService } = require('../../../src/schedule/schedule-service');

const projectId = 'demo-financeai-integration';
let environment;
let db;
before(async () => {
    assert.equal(process.env.FIRESTORE_EMULATOR_HOST, '127.0.0.1:8080', 'An explicit local emulator is required');
    environment = await initializeTestEnvironment({ projectId,
        firestore: { host: '127.0.0.1', port: 8080, rules:
            "rules_version = '2'; service cloud.firestore { match /databases/{database}/documents { match /{document=**} { allow read, write: if true; } } }"
        }
    });
    // This open rule set belongs only to the isolated demo project, never to the application project.
    db = environment.authenticatedContext('A').firestore();
});
beforeEach(async () => { await environment.clearFirestore(); });
after(async () => { if (environment) await environment.cleanup(); });

test('real Firestore retries concurrent completions and commits one financial winner', { timeout: 30_000 }, async () => {
    const day = Date.parse('2026-10-09T00:00:00Z');
    const plan = { userId: 'A', amountMinor: 2550, amount: 25.5, currencyCode: 'USD',
        type: 'EXPENSE', category: 'FOOD', note: 'receipt', scheduledDate: day, revision: 1, deleted: false };
    await db.doc('users/A').set({ currencyCode: 'USD', timeZoneId: 'UTC' });
    await db.doc('scheduled_transactions/p1').set(plan);
    const request = requestedAt => ({ type: 'complete', userId: 'A', transactionId: 'p1',
        scheduledDate: day, requestedAt, plan, processed: false });
    await db.doc('schedule_commands/first').set(request(day + 1000));
    await db.doc('schedule_commands/second').set(request(day + 2000));

    let release;
    const barrier = new Promise(resolve => { release = resolve; });
    let firstAttempts = 0;
    let attempts = 0;
    const coordinated = {
        collection: name => db.collection(name),
        runTransaction: block => {
            let first = true;
            return db.runTransaction(async nativeTransaction => {
                attempts++;
                const initial = first;
                first = false;
                const result = await block(nativeTransaction);
                if (initial) {
                    firstAttempts++;
                    if (firstAttempts === 2) release();
                    await barrier;
                }
                return result;
            });
        }
    };
    const service = scheduleService(coordinated, () => day + 3000);
    await Promise.all([service.processCommand('first'), service.processCommand('second')]);
    assert.ok(attempts >= 3, 'At least one SDK transaction must retry after the forced contention');
    const financial = await db.collection('transactions').get();
    assert.equal(financial.size, 1);
    assert.equal(financial.docs[0].id, 'completed_p1');
    assert.equal(financial.docs[0].data().amountMinor, 2550);
    assert.ok([day + 1000, day + 2000].includes(financial.docs[0].data().date));
    const outcomes = await Promise.all(['first', 'second'].map(id => db.doc('schedule_commands/' + id).get()));
    assert.deepEqual(outcomes.map(row => row.data().outcome).sort(), ['already_applied', 'applied']);
    assert.equal((await db.doc('scheduled_transactions/p1').get()).data().deleted, true);
    assert.equal((await db.collection('notification_events').get()).size, 1);
});
