'use strict';

const { FinancialFields: F } = require('../financial-contract');

const ScheduleCommandType = Object.freeze({
    COMPLETE: 'complete', SNOOZE: 'snooze', EXPIRATION_SHOWN: 'expiration_shown'
});
const ScheduleStatus = Object.freeze({ ACTIVE: 'active', COMPLETED: 'completed', DELETED: 'deleted' });
const CommandOutcome = Object.freeze({
    APPLIED: 'applied', ALREADY_APPLIED: 'already_applied', STALE: 'stale', TERMINAL: 'terminal',
    EXPIRED: 'expired', EARLY: 'early', INVALID: 'invalid', MISSING: 'missing', FORBIDDEN: 'forbidden',
    CONFLICT: 'conflict', INVALID_MONEY: 'invalid_money'
});
const ScheduleCollections = Object.freeze({
    PLANS: 'scheduled_transactions', STATES: 'schedule_states', COMMANDS: 'schedule_commands',
    EVENTS: 'notification_events', FINANCIAL: 'transactions'
});
const PlanFields = Object.freeze({
    TYPE: 'type', SCHEDULED_DATE: 'scheduledDate', NOTIFICATION_SENT: 'notificationSent',
    EXPIRATION_NOTIFICATION_SENT: 'expirationNotificationSent'
});

// Deliberate business allowlists. Never replace them with Object.values(FinancialFields).
const COMPLETABLE_PLAN_FIELDS = Object.freeze([
    F.AMOUNT_MINOR, F.CURRENCY_CODE, F.LEGACY_AMOUNT, PlanFields.TYPE, F.CATEGORY, F.NOTE,
    PlanFields.SCHEDULED_DATE, F.LOCATION_FULL, F.LOCATION_SHORT, F.LATITUDE, F.LONGITUDE,
    F.PHOTO_STORAGE_URL, F.PHOTO_REMOVED, F.PHOTO_VERSION, F.PHOTO_INTENT
]);
const COMPLETED_PLAN_EDIT_FIELDS = Object.freeze(
    COMPLETABLE_PLAN_FIELDS.filter(field => field !== PlanFields.SCHEDULED_DATE)
);

module.exports = { ScheduleCommandType, ScheduleStatus, CommandOutcome, ScheduleCollections,
    PlanFields, COMPLETABLE_PLAN_FIELDS, COMPLETED_PLAN_EDIT_FIELDS };
