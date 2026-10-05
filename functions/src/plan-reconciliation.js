'use strict';

const { FinancialFields } = require('./financial-contract');
const { PlanFields, COMPLETABLE_PLAN_FIELDS, COMPLETED_PLAN_EDIT_FIELDS } = require('./schedule-contract');
function normalize(plan) {
    return Object.fromEntries(COMPLETABLE_PLAN_FIELDS.map(field => [field, plan?.[field] ?? null]));
}
function reconcilePlan(base, local, remote) {
    if (!remote) return normalize(local);
    const before = normalize(base), wanted = normalize(local), current = normalize(remote);
    const result = {};
    for (const field of COMPLETABLE_PLAN_FIELDS) {
        const localChanged = wanted[field] !== before[field];
        const remoteChanged = current[field] !== before[field];
        if (localChanged && remoteChanged && wanted[field] !== current[field]) return null;
        result[field] = localChanged ? wanted[field] : current[field];
    }
    return result;
}
function completedPlanNeedsChoice(base, wanted, financial) {
    const current = { ...financial, [PlanFields.TYPE]: financial[FinancialFields.TRANSACTION] };
    return COMPLETED_PLAN_EDIT_FIELDS.some(field => {
        const before = field === FinancialFields.NOTE ? base?.[field] || '' : base?.[field] ?? null;
        const mine = field === FinancialFields.NOTE ? wanted?.[field] || '' : wanted?.[field] ?? null;
        const theirs = field === FinancialFields.NOTE ? current[field] || '' : current[field] ?? null;
        return mine !== before && mine !== theirs;
    });
}
module.exports = { reconcilePlan, completedPlanNeedsChoice };
