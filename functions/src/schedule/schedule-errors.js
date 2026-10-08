'use strict';

const ScheduleErrorCode = Object.freeze({
    INVALID_TIME_ZONE: 'INVALID_TIME_ZONE',
    OWNER_MISMATCH: 'OWNER_MISMATCH',
    INVALID_REQUEST_TIME: 'INVALID_REQUEST_TIME',
    INVALID_COMMAND: 'INVALID_COMMAND',
    FCM_TRANSIENT_FAILURE: 'FCM_TRANSIENT_FAILURE',
    AUTHENTICATION_REQUIRED: 'AUTHENTICATION_REQUIRED',
    DEVICE_NOT_REGISTERED: 'DEVICE_NOT_REGISTERED',
    INVALID_RESTORE_CURSOR: 'INVALID_RESTORE_CURSOR'
});

class ScheduleError extends Error {
    constructor(code) {
        super(code);
        this.name = 'ScheduleError';
        this.code = code;
    }
}

function scheduleRestoreFailure(error) {
    if (!(error instanceof ScheduleError)) return null;
    switch (error.code) {
    case ScheduleErrorCode.AUTHENTICATION_REQUIRED:
        return { code: 'unauthenticated', message: 'Authentication required' };
    case ScheduleErrorCode.DEVICE_NOT_REGISTERED:
        return { code: 'permission-denied', message: 'Device is not registered to this account' };
    case ScheduleErrorCode.INVALID_RESTORE_CURSOR:
        return { code: 'invalid-argument', message: 'Invalid restore cursor' };
    default:
        return null; // Unexpected infrastructure errors must retain their retry semantics.
    }
}

module.exports = { ScheduleError, ScheduleErrorCode, scheduleRestoreFailure };
