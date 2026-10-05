'use strict';

const { ScheduleCollections } = require('./schedule-contract');
const { ScheduleError, ScheduleErrorCode } = require('./schedule-errors');

// Reconnection restores authoritative state; it never bypasses the 09/18/snooze policy.
function scheduleRestore(db, service, documentIdField) {
    return async (data, context) => {
        if (!context.auth) throw new ScheduleError(ScheduleErrorCode.AUTHENTICATION_REQUIRED);
        const user = await db.collection('users').doc(context.auth.uid).get();
        if (typeof data?.deviceToken !== 'string' || !(user.data()?.fcmTokens || []).includes(data.deviceToken))
            throw new ScheduleError(ScheduleErrorCode.DEVICE_NOT_REGISTERED);
        if (data.cursor != null && (typeof data.cursor !== 'string' || data.cursor.includes('/') || data.cursor.length > 512))
            throw new ScheduleError(ScheduleErrorCode.INVALID_RESTORE_CURSOR);
        let query = db.collection(ScheduleCollections.PLANS).where('userId', '==', context.auth.uid)
            .orderBy(documentIdField).limit(100);
        if (data.cursor) query = query.startAfter(data.cursor);
        const plans = await query.get();
        for (const plan of plans.docs) if (!plan.data().deleted) await service.refresh(plan.id);
        return { success: true, count: plans.size, nextCursor: plans.size === 100 ? plans.docs.at(-1).id : null };
    };
}

module.exports = { scheduleRestore };
