'use strict';

const functions = require('firebase-functions/v1');
const admin = require('firebase-admin');
const { createHash } = require('node:crypto');
const { scheduleService } = require('./src/schedule/schedule-service');
const { authLookups } = require('./src/auth/auth-lookups');
const { authLookupFailure } = require('./src/auth/auth-errors');
const { ScheduleError, ScheduleErrorCode, scheduleRestoreFailure } = require('./src/schedule/schedule-errors');
const { ScheduleCommandType, ScheduleStatus, ScheduleCollections } = require('./src/schedule/schedule-contract');
const { scheduleRestore } = require('./src/schedule/schedule-restore');

admin.initializeApp();
const db = admin.firestore();
const service = scheduleService(db);
const lookups = authLookups(db, admin.auth());
const preLogin = functions.region('us-central1').runWith({ enforceAppCheck: true, maxInstances: 3 });
const accountCheck = reset => async (data, context) => {
    try { return await lookups.check(data, context, reset); }
    catch (error) {
        const failure = authLookupFailure(error);
        throw new functions.https.HttpsError(failure.code, failure.message);
    }
};
exports.checkRegisteredAccount = preLogin.https.onCall(accountCheck(false));
exports.checkPasswordResetIdentity = preLogin.https.onCall(accountCheck(true));
const background = functions.region('us-central1').runWith({ memory: '256MB', maxInstances: 3, failurePolicy: true });

// Keep existing trigger identities. An onDelete trigger cannot be changed to onUpdate in place.
exports.sendScheduledNotification = background.firestore.document(ScheduleCollections.PLANS + '/{transactionId}')
    .onCreate((_, context) => service.refresh(context.params.transactionId));
exports.onScheduledTransactionChanged = background.firestore.document(ScheduleCollections.PLANS + '/{transactionId}')
    .onUpdate((change, context) => {
        const before = change.before.data(), after = change.after.data();
        return before.deleted !== after.deleted || before.scheduledDate !== after.scheduledDate
            ? service.refresh(context.params.transactionId) : null;
    });
exports.onScheduleCommand = background.firestore.document(ScheduleCollections.COMMANDS + '/{commandId}')
    .onCreate((_, context) => service.processCommand(context.params.commandId));

async function sendEvent(eventRef) {
    const eventDoc = await eventRef.get();
    if (!eventDoc.exists || eventDoc.data().sent) return;
    const event = eventDoc.data();
    if (typeof event.userId !== 'string' || typeof event.transactionId !== 'string' || !Number.isSafeInteger(event.revision)) {
        await eventRef.update({ sent: true, failure: 'invalid_event' });
        return;
    }
    const userRef = db.collection('users').doc(event.userId);
    const user = await userRef.get();
    const delivered = new Set(event.delivered || []);
    const storedTokens = user.data()?.fcmTokens;
    const tokens = [...new Set(Array.isArray(storedTokens) ? storedTokens.filter(token => typeof token === 'string' && token.length > 0) : [])];
    let transientFailure = false;
    for (const token of tokens) {
        const identity = createHash('sha256').update(token).digest('hex');
        if (delivered.has(identity)) continue;
        try {
            await admin.messaging().send({ token, data: { type: 'SCHEDULE_STATE_CHANGED',
                userId: event.userId, transactionId: event.transactionId, eventId: eventDoc.id,
                revision: String(event.revision) }, android: { priority: 'normal' } });
            await eventRef.update({ delivered: admin.firestore.FieldValue.arrayUnion(identity) });
        } catch (error) {
            if (['messaging/invalid-registration-token', 'messaging/registration-token-not-registered', 'messaging/invalid-argument'].includes(error.code))
                await userRef.update({ fcmTokens: admin.firestore.FieldValue.arrayRemove(token) });
            else transientFailure = true;
        }
    }
    if (transientFailure) throw new ScheduleError(ScheduleErrorCode.FCM_TRANSIENT_FAILURE);
    await eventRef.update({ sent: true });
}

exports.onNotificationEvent = background.firestore.document(ScheduleCollections.EVENTS + '/{eventId}')
    .onCreate(snap => sendEvent(snap.ref));

// Existing scheduled job is retained; bounded batches avoid unbounded account-wide reads.
exports.checkExpiredReminders = functions.region('us-central1').runWith({ memory: '256MB', maxInstances: 1 })
    .pubsub.schedule('every 5 minutes').onRun(async () => {
        const due = await db.collection(ScheduleCollections.STATES).where('dueAt', '<=', Date.now()).limit(100).get();
        for (const doc of due.docs) await service.advance(doc.id);
        const events = await db.collection(ScheduleCollections.EVENTS).where('sent', '==', false).limit(100).get();
        for (const event of events.docs) await sendEvent(event.ref);
    });

const restore = scheduleRestore(db, service, admin.firestore.FieldPath.documentId());
const restoreSchedules = async (data, context) => {
    try { return await restore(data, context); }
    catch (error) {
        const failure = scheduleRestoreFailure(error);
        if (failure) throw new functions.https.HttpsError(failure.code, failure.message);
        throw error;
    }
};
exports.sendPendingNotifications = functions.region('us-central1').runWith({ maxInstances: 3 }).https.onCall(restoreSchedules);
exports.restoreScheduleState = functions.region('us-central1').runWith({ enforceAppCheck: true, maxInstances: 3 }).https.onCall(restoreSchedules);

// Compatibility adapters for old deployed trigger identities; new clients use schedule_commands.
exports.manualSendToAllDevices = background.firestore.document('notification_triggers/{triggerId}').onCreate(async snap => {
    const data = snap.data();
    const plan = await db.collection(ScheduleCollections.PLANS).doc(data.transactionId).get();
    if (plan.exists && data.userId === plan.data().userId && !plan.data().deleted) await service.refresh(plan.id);
    await snap.ref.delete();
});
exports.scheduleReminderTask = background.firestore.document('notification_reminders/{reminderId}').onCreate(async snap => {
    const data = snap.data();
    const plan = await db.collection(ScheduleCollections.PLANS).doc(data.transactionId).get();
    if (plan.exists && data.userId === plan.data().userId && !plan.data().deleted) {
        const ref = db.collection(ScheduleCollections.COMMANDS).doc('legacy_' + snap.id);
        await db.runTransaction(async tx => {
            if (!(await tx.get(ref)).exists) tx.create(ref, { type: ScheduleCommandType.SNOOZE, userId: data.userId,
                transactionId: plan.id, scheduledDate: plan.data().scheduledDate, requestedAt: Date.now(), processed: false });
        });
        await service.processCommand(ref.id);
    }
    await snap.ref.delete();
});
exports.onNotificationDismissed = background.firestore.document('notification_dismissals/{dismissalId}')
    .onCreate(snap => snap.ref.delete());
exports.onScheduledTransactionDelete = background.firestore.document(ScheduleCollections.PLANS + '/{transactionId}')
    .onDelete(async (snap, context) => {
        const stateRef = db.collection(ScheduleCollections.STATES).doc(context.params.transactionId);
        await db.runTransaction(async tx => {
            const old = await tx.get(stateRef);
            const revision = (old.data()?.revision || 0) + 1;
            tx.set(stateRef, { ...(old.data() || {}), userId: snap.data().userId,
                scheduledDate: snap.data().scheduledDate, status: ScheduleStatus.DELETED, dueAt: null, snoozeAt: null, revision });
            tx.set(db.collection(ScheduleCollections.EVENTS).doc(context.params.transactionId + '_' + revision), {
                userId: snap.data().userId, transactionId: context.params.transactionId, revision, createdAt: Date.now(), sent: false });
        });
    });
