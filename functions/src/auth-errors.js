'use strict';

const AuthLookupErrorCode = Object.freeze({
    APP_CHECK_REQUIRED: 'APP_CHECK_REQUIRED',
    RATE_LIMITED: 'RATE_LIMITED',
    INVALID_INPUT: 'INVALID_INPUT'
});

class AuthLookupError extends Error {
    constructor(code) {
        super(code);
        this.name = 'AuthLookupError';
        this.code = code;
    }
}

// Callable responses are deliberately generic; diagnostic messages never select a response.
function authLookupFailure(error) {
    if (error instanceof AuthLookupError) {
        switch (error.code) {
        case AuthLookupErrorCode.RATE_LIMITED:
            return { code: 'resource-exhausted', message: 'Try again later' };
        case AuthLookupErrorCode.INVALID_INPUT:
            return { code: 'invalid-argument', message: 'Invalid account check' };
        case AuthLookupErrorCode.APP_CHECK_REQUIRED:
            return { code: 'failed-precondition', message: 'App verification required' };
        }
    }
    return { code: 'internal', message: 'Account check failed' };
}

module.exports = { AuthLookupError, AuthLookupErrorCode, authLookupFailure };
