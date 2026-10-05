'use strict';

const { createHash } = require('node:crypto');
const { AuthLookupError, AuthLookupErrorCode } = require('./auth-errors');

function authLookups(db, auth, now = Date.now) {
    async function consume(context) {
        if (!context.app) throw new AuthLookupError(AuthLookupErrorCode.APP_CHECK_REQUIRED);
        const key = createHash('sha256').update(context.app.appId + '|' + (context.rawRequest?.ip || 'unknown')).digest('hex');
        const ref = db.collection('auth_lookup_limits').doc(key);
        await db.runTransaction(async tx => {
            const old = (await tx.get(ref)).data();
            const window = Math.floor(now() / 60_000);
            const count = old?.window === window ? old.count + 1 : 1;
            if (count > 10) throw new AuthLookupError(AuthLookupErrorCode.RATE_LIMITED);
            tx.set(ref, { window, count });
        });
    }
    async function check(data, context, reset) {
        await consume(context);
        if (typeof data?.email !== 'string' || data.email.length > 254 || !data.email.includes('@'))
            throw new AuthLookupError(AuthLookupErrorCode.INVALID_INPUT);
        if (reset) {
            if (typeof data.firstName !== 'string' || typeof data.lastName !== 'string' ||
                data.firstName.length > 100 || data.lastName.length > 100)
                throw new AuthLookupError(AuthLookupErrorCode.INVALID_INPUT);
        }
        let user;
        try { user = await auth.getUserByEmail(data.email); }
        catch (error) {
            if (error.code === 'auth/user-not-found') return { found: false };
            if (error.code === 'auth/invalid-email') throw new AuthLookupError(AuthLookupErrorCode.INVALID_INPUT);
            throw error;
        }
        const profile = await db.collection('users').doc(user.uid).get();
        return { found: profile.exists && (!reset || (profile.data().firstName === data.firstName && profile.data().lastName === data.lastName)) };
    }
    return { check };
}
module.exports = { authLookups };
