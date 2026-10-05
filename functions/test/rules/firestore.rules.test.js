'use strict';

const { before, beforeEach, after, test } = require('node:test');
const fs = require('node:fs');
const { initializeTestEnvironment, assertSucceeds, assertFails } = require('@firebase/rules-unit-testing');
const { doc, setDoc, updateDoc, getDoc, getDocs, collection, query, where } = require('firebase/firestore');
let environment;
before(async () => {
    if (!process.env.FIRESTORE_EMULATOR_HOST) throw new Error('LOCAL_EMULATOR_REQUIRED');
    environment = await initializeTestEnvironment({ projectId: 'demo-financeai',
        firestore: { rules: fs.readFileSync('../firestore.rules', 'utf8') } });
});
beforeEach(async () => {
    await environment.clearFirestore();
    await environment.withSecurityRulesDisabled(async context => {
        const db = context.firestore();
        await setDoc(doc(db, 'users/A'), { currencyCode: 'TRY', timeZoneId: 'Europe/Istanbul' });
        await setDoc(doc(db, 'scheduled_transactions/p1'), { userId: 'A', scheduledDate: 100, deleted: false });
        await setDoc(doc(db, 'transactions/t1'), { userId: 'A', deleted: false });
        await setDoc(doc(db, 'schedule_states/p1'), { userId: 'A', status: 'active' });
    });
});
after(async () => { if (environment) await environment.cleanup(); });
const db = uid => uid ? environment.authenticatedContext(uid).firestore() : environment.unauthenticatedContext().firestore();
test('unauthenticated clients cannot read or write finance/profile data', async () => {
    await assertFails(getDoc(doc(db(), 'transactions/t1')));
    await assertFails(setDoc(doc(db(), 'transactions/attack'), { userId: 'A' }));
    await assertFails(getDoc(doc(db(), 'users/A')));
});
test('another signed-in account cannot read, write or steal an existing record', async () => {
    await assertFails(getDoc(doc(db('B'), 'transactions/t1')));
    await assertFails(updateDoc(doc(db('B'), 'transactions/t1'), { userId: 'B' }));
    await assertFails(setDoc(doc(db('B'), 'transactions/t2'), { userId: 'A' }));
});
test('owner queries work while unrestricted collection queries are denied', async () => {
    await assertSucceeds(getDocs(query(collection(db('A'), 'transactions'), where('userId', '==', 'A'))));
    await assertFails(getDocs(collection(db('A'), 'transactions')));
});
test('missing-document reads required by create transactions are allowed without exposing existing foreign records', async () => {
    await assertSucceeds(getDoc(doc(db('A'), 'transactions/new')));
    await assertSucceeds(getDoc(doc(db('A'), 'schedule_commands/new')));
    await assertSucceeds(setDoc(doc(db('A'), 'transactions/new'), { userId: 'A', deleted: false }));
});
test('ordinary owner conflict recovery remains possible but completed plans cannot be reopened', async () => {
    await assertSucceeds(updateDoc(doc(db('A'), 'transactions/t1'), { deleted: true }));
    await assertSucceeds(updateDoc(doc(db('A'), 'transactions/t1'), { deleted: false }));
    await environment.withSecurityRulesDisabled(async context => {
        await updateDoc(doc(context.firestore(), 'scheduled_transactions/p1'), { deleted: true, completedFrom: 'completed_p1' });
    });
    await assertFails(updateDoc(doc(db('A'), 'scheduled_transactions/p1'), { deleted: false }));
});
test('owned plans can be created, edited and tombstoned without granting server-only fields', async () => {
    const plan = doc(db('A'), 'scheduled_transactions/new-plan');
    await assertSucceeds(setDoc(plan, { userId: 'A', scheduledDate: 100, deleted: false, note: 'initial' }));
    await assertSucceeds(updateDoc(plan, { note: 'edited', scheduledDate: 200 }));
    await assertSucceeds(updateDoc(plan, { deleted: true }));
    await assertSucceeds(updateDoc(plan, { deleted: false }));
    await assertFails(updateDoc(plan, { completedFrom: 'completed_new-plan' }));
    await assertFails(updateDoc(plan, { previousMutationId: 'forged' }));
    await assertFails(setDoc(doc(db('A'), 'scheduled_transactions/forged-mutation'), {
        userId: 'A', previousMutationId: 'forged'
    }));
});
test('account zone/currency are first-write-only and profiles are not enumerable', async () => {
    await assertSucceeds(getDoc(doc(db('A'), 'users/A')));
    await assertFails(updateDoc(doc(db('A'), 'users/A'), { timeZoneId: 'Europe/Berlin' }));
    await assertFails(updateDoc(doc(db('A'), 'users/A'), { currencyCode: 'USD' }));
    await assertFails(getDocs(query(collection(db('A'), 'users'), where('email', '==', 'person@example.test'))));
});
test('only owned plan commands can be created; receipts and shared state are server-only', async () => {
    const value = { userId: 'A', transactionId: 'p1', type: 'snooze', scheduledDate: 100, requestedAt: 100, processed: false };
    await assertSucceeds(setDoc(doc(db('A'), 'schedule_commands/c1'), value));
    await assertFails(setDoc(doc(db('B'), 'schedule_commands/c2'), { ...value, userId: 'B' }));
    await assertFails(updateDoc(doc(db('A'), 'schedule_commands/c1'), { processed: true }));
    await assertFails(updateDoc(doc(db('A'), 'schedule_states/p1'), { status: 'completed' }));
    await assertFails(setDoc(doc(db('A'), 'notification_events/e1'), { userId: 'A' }));
});
test('offline create-and-complete requires an owned candidate plan', async () => {
    const value = { userId: 'A', transactionId: 'new-plan', type: 'complete', scheduledDate: 100, requestedAt: 100, processed: false };
    await assertSucceeds(setDoc(doc(db('A'), 'schedule_commands/c1'), { ...value, plan: { userId: 'A' } }));
    await assertFails(setDoc(doc(db('A'), 'schedule_commands/c2'), { ...value, plan: { userId: 'B' } }));
    await assertFails(setDoc(doc(db('A'), 'transactions/completed_new-plan'), { userId: 'A' }));
    await assertFails(setDoc(doc(db('A'), 'scheduled_transactions/forged-link'), { userId: 'A', completedFrom: 'foreign' }));
});
