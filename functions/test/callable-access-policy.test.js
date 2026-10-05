'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');
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
