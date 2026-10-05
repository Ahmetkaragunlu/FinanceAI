'use strict';

const { DateTime } = require('luxon');
const { ScheduleCommandType, ScheduleStatus, CommandOutcome } = require('./schedule-contract');
const { ScheduleError, ScheduleErrorCode } = require('./schedule-errors');
const HOUR = 60 * 60 * 1000;
const RETENTION = 24 * HOUR;

function dayTimes(scheduledDate, zone) {
    const day = DateTime.fromMillis(scheduledDate, { zone }).startOf('day');
    if (!day.isValid) throw new ScheduleError(ScheduleErrorCode.INVALID_TIME_ZONE);
    return { morning: day.set({ hour: 9 }).toMillis(), evening: day.set({ hour: 18 }).toMillis(),
        end: day.plus({ days: 1 }).toMillis() };
}

// All transitions are pure; Firestore transactions provide cross-device serialization.
function initialState(plan, zone) {
    const { morning } = dayTimes(plan.scheduledDate, zone);
    return { userId: plan.userId, scheduledDate: plan.scheduledDate,
        status: plan.deleted ? ScheduleStatus.DELETED : ScheduleStatus.ACTIVE,
        snoozeAt: null, expirationAcceptedAt: null, deleteAt: null, dueAt: plan.deleted ? null : morning,
        automaticSlots: 0, revision: 0 };
}

function transition(plan, state, command, now, zone) {
    if (command.userId !== plan.userId) throw new ScheduleError(ScheduleErrorCode.OWNER_MISMATCH);
    if (command.scheduledDate !== plan.scheduledDate) return { state, outcome: CommandOutcome.STALE };
    if (plan.deleted || state.status !== ScheduleStatus.ACTIVE) return { state, outcome: CommandOutcome.TERMINAL };
    if (state.reactivatedAt != null && command.requestedAt < state.reactivatedAt)
        return { state, outcome: CommandOutcome.STALE };
    const { end } = dayTimes(plan.scheduledDate, zone);
    if (command.type === ScheduleCommandType.SNOOZE) {
        if (now >= end || state.expirationAcceptedAt != null) return { state, outcome: CommandOutcome.EXPIRED };
        // Never replay an old offline snooze as a brand-new hour after reconnect.
        if (!Number.isSafeInteger(command.requestedAt) || command.requestedAt > now + 5 * 60 * 1000)
            throw new ScheduleError(ScheduleErrorCode.INVALID_REQUEST_TIME);
        const due = Math.min(command.requestedAt + HOUR, end);
        if (state.lastSnoozeRequestedAt != null && command.requestedAt < state.lastSnoozeRequestedAt)
            return { state, outcome: CommandOutcome.STALE };
        return { state: { ...state, snoozeAt: due, dueAt: due,
            lastSnoozeRequestedAt: command.requestedAt, revision: state.revision + 1 }, outcome: CommandOutcome.APPLIED };
    }
    if (command.type === ScheduleCommandType.EXPIRATION_SHOWN) {
        if (now < end) return { state, outcome: CommandOutcome.EARLY };
        // The first server acceptance starts retention, not the device clock/FCM delivery.
        if (state.expirationAcceptedAt != null) return { state, outcome: CommandOutcome.ALREADY_APPLIED };
        return { state: { ...state, snoozeAt: null, expirationAcceptedAt: now,
            deleteAt: now + RETENTION, dueAt: now + RETENTION, revision: state.revision + 1 },
            outcome: CommandOutcome.APPLIED };
    }
    throw new ScheduleError(ScheduleErrorCode.INVALID_COMMAND);
}

function tick(state, now, zone) {
    if (state.status !== ScheduleStatus.ACTIVE || state.dueAt == null || now < state.dueAt) return null;
    if (state.deleteAt != null) return now >= state.deleteAt ? { ...state, status: ScheduleStatus.DELETED, dueAt: null,
        revision: state.revision + 1 } : null;
    const { morning, evening, end } = dayTimes(state.scheduledDate, zone);
    // At day end ask devices to evaluate expiration; never assert that they showed it.
    const slots = now >= evening ? 3 : now >= morning ? 1 : 0;
    return { ...state, automaticSlots: state.automaticSlots | slots, snoozeAt: null,
        dueAt: now >= end ? null : now < evening ? evening : end, revision: state.revision + 1 };
}

module.exports = { HOUR, RETENTION, dayTimes, initialState, transition, tick };
