'use strict';

const { initialState, transition, tick } = require('./reminder-policy');
const { reconcilePlan, completedPlanNeedsChoice } = require('./plan-reconciliation');
const { ScheduleCommandType, ScheduleStatus, CommandOutcome, ScheduleCollections, PlanFields } = require('./schedule-contract');
const { DateTime } = require('luxon');

function validZone(zone) { return typeof zone === 'string' && DateTime.now().setZone(zone).isValid; }

function scheduleService(db, now = Date.now) {
    const stateRef = id => db.collection(ScheduleCollections.STATES).doc(id);
    const emit = (tx, id, state) => {
        const eventId = id + '_' + state.revision;
        tx.set(db.collection(ScheduleCollections.EVENTS).doc(eventId), {
            userId: state.userId, transactionId: id, revision: state.revision,
            createdAt: now(), sent: false
        });
    };

    async function refresh(id) {
        await db.runTransaction(async tx => {
            const planRef = db.collection(ScheduleCollections.PLANS).doc(id);
            const [planDoc, oldDoc] = await Promise.all([tx.get(planRef), tx.get(stateRef(id))]);
            if (!planDoc.exists) return;
            const plan = planDoc.data();
            const user = await tx.get(db.collection('users').doc(plan.userId));
            const zone = user.data()?.timeZoneId;
            if (!validZone(zone) || !Number.isSafeInteger(plan.scheduledDate)) return;
            const old = oldDoc.exists ? oldDoc.data() : null;
            let next = old && old.scheduledDate === plan.scheduledDate ? old : initialState(plan, zone);
            if (plan.deleted) next = { ...next,
                status: plan.completedFrom ? ScheduleStatus.COMPLETED : ScheduleStatus.DELETED, dueAt: null, snoozeAt: null };
            else if (old?.status === ScheduleStatus.COMPLETED) return; // a completed financial operation cannot be reopened
            else if (old?.status === ScheduleStatus.DELETED) next = { ...initialState(plan, zone), reactivatedAt: now() };
            next = { ...next, revision: (old?.revision || 0) + 1 };
            tx.set(stateRef(id), next);
            emit(tx, id, next);
        });
    }

    async function processCommand(id) {
        await db.runTransaction(async tx => {
            const ref = db.collection(ScheduleCollections.COMMANDS).doc(id);
            const doc = await tx.get(ref);
            if (!doc.exists || doc.data().processed) return;
            const command = doc.data();
            if (!Object.values(ScheduleCommandType).includes(command.type) ||
                typeof command.userId !== 'string' || typeof command.transactionId !== 'string' ||
                !command.transactionId || command.transactionId.includes('/') ||
                !Number.isSafeInteger(command.scheduledDate) || !Number.isSafeInteger(command.requestedAt) ||
                command.requestedAt > now() + 5 * 60 * 1000) {
                tx.update(ref, { processed: true, outcome: CommandOutcome.INVALID, acceptedAt: now() });
                return;
            }
            const planRef = db.collection(ScheduleCollections.PLANS).doc(command.transactionId);
            const completionRef = db.collection(ScheduleCollections.FINANCIAL).doc('completed_' + command.transactionId);
            const sharedRef = stateRef(command.transactionId);
            const [planDoc, sharedDoc, userDoc, completedDoc] = await Promise.all([
                tx.get(planRef), tx.get(sharedRef), tx.get(db.collection('users').doc(command.userId)),
                tx.get(completionRef)
            ]);
            // Local creation followed by offline completion may never have reached Firestore.
            const plan = planDoc.exists ? planDoc.data() : command.type === ScheduleCommandType.COMPLETE ? command.plan : null;
            const zone = userDoc.data()?.timeZoneId;
            if (!plan || !validZone(zone) || !Number.isSafeInteger(plan.scheduledDate)) {
                tx.update(ref, { processed: true, outcome: CommandOutcome.MISSING, acceptedAt: now() });
                return;
            }
            if (plan.userId !== command.userId) {
                tx.update(ref, { processed: true, outcome: CommandOutcome.FORBIDDEN, acceptedAt: now() });
                return;
            }
            let state = sharedDoc.exists ? sharedDoc.data() : initialState(plan, zone);
            if (state.scheduledDate !== plan.scheduledDate) state = initialState(plan, zone);
            let outcome;
            if (command.type === ScheduleCommandType.COMPLETE) {
                if (completedDoc.exists) {
                    if (completedDoc.data().userId !== command.userId) {
                        tx.update(ref, { processed: true, outcome: CommandOutcome.FORBIDDEN, acceptedAt: now() });
                        return;
                    }
                    if (completedPlanNeedsChoice(command.base, command.plan, completedDoc.data())) {
                        tx.update(ref, { processed: true, outcome: CommandOutcome.CONFLICT,
                            conflictTarget: ScheduleCollections.FINANCIAL, acceptedAt: now() });
                        return;
                    }
                    outcome = CommandOutcome.ALREADY_APPLIED;
                } else if (plan.deleted || state.status !== ScheduleStatus.ACTIVE) outcome = CommandOutcome.TERMINAL;
                else if (state.reactivatedAt != null && command.requestedAt < state.reactivatedAt) {
                    tx.update(ref, { processed: true, outcome: CommandOutcome.CONFLICT, acceptedAt: now() });
                    return;
                }
                else if (command.scheduledDate !== plan.scheduledDate) {
                    tx.update(ref, { processed: true, outcome: CommandOutcome.CONFLICT, acceptedAt: now() });
                    return;
                }
                else {
                    const reconciled = reconcilePlan(command.base, command.plan, planDoc.exists ? plan : null);
                    if (reconciled == null) {
                        tx.update(ref, { processed: true, outcome: CommandOutcome.CONFLICT, acceptedAt: now() });
                        return;
                    }
                    if (!Number.isSafeInteger(reconciled.amountMinor) || reconciled.amountMinor <= 0 ||
                        reconciled.currencyCode !== userDoc.data().currencyCode) {
                        tx.update(ref, { processed: true, outcome: CommandOutcome.INVALID_MONEY, acceptedAt: now() });
                        return;
                    }
                    const financial = { ...reconciled, userId: plan.userId, transaction: reconciled.type, note: reconciled.note || '',
                        date: command.requestedAt, completedFromScheduledId: command.transactionId,
                        revision: 1, mutationId: id, deleted: false };
                    delete financial[PlanFields.SCHEDULED_DATE];
                    delete financial[PlanFields.TYPE];
                    delete financial[PlanFields.NOTIFICATION_SENT];
                    delete financial[PlanFields.EXPIRATION_NOTIFICATION_SENT];
                    tx.set(completionRef, financial);
                    tx.set(planRef, { ...plan, deleted: true, completedFrom: completionRef.id,
                        previousMutationId: plan.mutationId || null,
                        revision: (plan.revision || 0) + 1, mutationId: id });
                    state = { ...state, status: ScheduleStatus.COMPLETED, snoozeAt: null, dueAt: null, revision: state.revision + 1 };
                    outcome = CommandOutcome.APPLIED;
                }
            } else {
                const result = transition(plan, state, command, now(), zone);
                state = result.state;
                outcome = result.outcome;
            }
            if (outcome === CommandOutcome.APPLIED) {
                tx.set(sharedRef, state);
                emit(tx, command.transactionId, state);
            }
            tx.update(ref, { processed: true, outcome, acceptedAt: now() });
        });
    }

    async function advance(id) {
        await db.runTransaction(async tx => {
            const ref = stateRef(id);
            const doc = await tx.get(ref);
            if (!doc.exists) return;
            const state = doc.data();
            const planRef = db.collection(ScheduleCollections.PLANS).doc(id);
            const [plan, user] = await Promise.all([tx.get(planRef), tx.get(db.collection('users').doc(state.userId))]);
            if (!plan.exists || plan.data().deleted || !validZone(user.data()?.timeZoneId) || !Number.isSafeInteger(state.scheduledDate)) {
                tx.update(ref, { dueAt: null }); // An invalid/terminal row must not starve the bounded due query.
                return;
            }
            const next = tick(state, now(), user.data()?.timeZoneId);
            if (!next) return;
            if (next.status === ScheduleStatus.DELETED) tx.update(planRef, { deleted: true,
                previousMutationId: plan.data().mutationId || null,
                revision: (plan.data().revision || 0) + 1, mutationId: 'expiration_' + id + '_' + next.revision });
            tx.set(ref, next);
            emit(tx, id, next);
        });
    }
    return { refresh, processCommand, advance };
}

module.exports = { scheduleService };
