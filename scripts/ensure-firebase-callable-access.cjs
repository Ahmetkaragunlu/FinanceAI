'use strict';

// Deployment-only repair for v1 callable resources left without an invoker binding.
// Defaults to read-only. It is not bundled into Android or the Functions source.
const project = 'financeai-7e7bc';
const region = 'us-central1';
const invokerRole = 'roles/cloudfunctions.invoker';
const targets = ['checkRegisteredAccount', 'checkPasswordResetIdentity', 'restoreScheduleState'];

function publicInvokerPolicy(current) {
    const bindings = current.bindings || [];
    // A default v1 getIamPolicy response can hide conditions; never overwrite such a policy.
    if (bindings.some(binding => binding.condition || binding.role.includes('_withcond_'))) {
        throw new Error('CONDITIONAL_IAM_POLICY_REQUIRES_REVIEW');
    }
    const next = bindings.map(binding => ({ ...binding, members: [...(binding.members || [])] }));
    const invoker = next.find(binding => binding.role === invokerRole);
    if (invoker) {
        if (!invoker.members.includes('allUsers')) invoker.members.push('allUsers');
    } else {
        next.push({ role: invokerRole, members: ['allUsers'] });
    }
    return { ...current, bindings: next }; // Preserve etag, existing principals and unrelated roles.
}

function canonicalBindings(policy) {
    return JSON.stringify((policy.bindings || []).map(binding => ({
        role: binding.role, members: [...(binding.members || [])].sort(), condition: binding.condition
    })).sort((a, b) => JSON.stringify(a).localeCompare(JSON.stringify(b))));
}

async function main(args) {
    if (args.length !== 2 && args.length !== 3) throw new Error('USE --project financeai-7e7bc [--apply]');
    if (args[0] !== '--project' || args[1] !== project || (args[2] && args[2] !== '--apply')) {
        throw new Error('UNAUTHORIZED_PROJECT_OR_ARGUMENT');
    }
    const apply = args[2] === '--apply';
    const auth = require('../functions/node_modules/firebase-tools/lib/auth');
    const { requireAuth } = require('../functions/node_modules/firebase-tools/lib/requireAuth');
    const gcf = require('../functions/node_modules/firebase-tools/lib/gcp/cloudfunctions');
    const account = auth.getGlobalDefaultAccount();
    if (!account) throw new Error('CLI_LOGIN_REQUIRED');
    const options = { project, nonInteractive: true };
    auth.setActiveAccount(options, account);
    await requireAuth(options);
    for (const id of targets) {
        const name = `projects/${project}/locations/${region}/functions/${id}`;
        const before = await gcf.getIamPolicy(name);
        const intended = publicInvokerPolicy(before);
        const changed = canonicalBindings(before) !== canonicalBindings(intended);
        if (apply && changed) await gcf.setIamPolicy({ name, policy: intended });
        const actual = apply ? await gcf.getIamPolicy(name) : before;
        if (apply && canonicalBindings(actual) !== canonicalBindings(intended)) {
            throw new Error(`IAM_RECHECK_MISMATCH:${id}`);
        }
        console.log(JSON.stringify({ id, mode: apply ? 'apply' : 'check',
            changed: apply && changed, needsUpdate: !apply && changed,
            publicInvoker: actual.bindings?.some(binding => binding.role === invokerRole && binding.members?.includes('allUsers')) || false }));
    }
}

module.exports = { publicInvokerPolicy };
if (require.main === module) {
    main(process.argv.slice(2)).catch(error => { console.error(error.message); process.exitCode = 1; });
}
