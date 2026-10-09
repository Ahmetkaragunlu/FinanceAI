'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { publicInvokerPolicy } = require('../../scripts/ensure-firebase-callable-access.cjs');

test('invoker repair adds only the function invocation role and preserves etag and unrelated roles', () => {
    const existing = { version: 1, etag: 'concurrency-token', bindings: [
        { role: 'roles/viewer', members: ['user:operator@example.invalid'] }
    ] };
    const repaired = publicInvokerPolicy(existing);
    assert.equal(repaired.etag, existing.etag);
    assert.equal(repaired.version, existing.version);
    assert.deepEqual(repaired.bindings[0], existing.bindings[0]);
    assert.deepEqual(repaired.bindings[1], { role: 'roles/cloudfunctions.invoker', members: ['allUsers'] });
    assert.equal(existing.bindings.length, 1);
});

test('existing invoker principals are retained without modifying the input policy', () => {
    const existing = { etag: 'token', bindings: [
        { role: 'roles/cloudfunctions.invoker', members: ['serviceAccount:caller@example.invalid'] }
    ] };
    const repaired = publicInvokerPolicy(existing);
    assert.deepEqual(repaired.bindings[0].members, ['serviceAccount:caller@example.invalid', 'allUsers']);
    assert.deepEqual(existing.bindings[0].members, ['serviceAccount:caller@example.invalid']);
});

test('invoker repair is idempotent and never duplicates the public principal', () => {
    const once = publicInvokerPolicy({ bindings: [] });
    assert.deepEqual(publicInvokerPolicy(once), once);
});

test('conditional policies require explicit review rather than a lossy rewrite', () => {
    for (const binding of [
        { role: 'roles/cloudfunctions.invoker', members: ['allUsers'], condition: { expression: 'true' } },
        { role: 'roles/viewer_withcond_hash', members: ['user:operator@example.invalid'] }
    ]) assert.throws(() => publicInvokerPolicy({ bindings: [binding] }), /CONDITIONAL_IAM_POLICY_REQUIRES_REVIEW/);
});

test('CLI failures report the failed operation without logging SDK account details', async () => {
    const source = fs.readFileSync(path.resolve(__dirname, '../../scripts/ensure-firebase-callable-access.cjs'), 'utf8');
    const privateDetail = 'synthetic-private-account-detail';
    const stderr = [];
    const cliModule = { exports: {} };
    const cliProcess = { argv: ['node', 'script', '--project', 'financeai-7e7bc'], exitCode: 0 };
    const sdkRequire = () => ({
        getGlobalDefaultAccount() { throw new Error(privateDetail); }
    });
    sdkRequire.main = cliModule;
    vm.runInNewContext(source, {
        require: sdkRequire, module: cliModule, process: cliProcess,
        console: { error: message => stderr.push(message), log: () => assert.fail('No IAM operation should run') }
    });
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(cliProcess.exitCode, 1);
    assert.equal(stderr.length, 1);
    assert.ok(stderr[0].includes('Callable access operation failed'));
    assert.ok(!stderr[0].includes(privateDetail));
});
