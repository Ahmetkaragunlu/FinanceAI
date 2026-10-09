'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { prepareAndroidConfig } = require('./prepare-android-config.cjs');

function workspace(t) {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), 'financeai-ci-config-'));
    fs.mkdirSync(path.join(root, 'app'));
    fs.writeFileSync(path.join(root, 'app', 'build.gradle.kts'), '// isolated test workspace\n');
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));
    return root;
}

test('ordinary local execution cannot create or replace client configuration', t => {
    const root = workspace(t);
    assert.throws(() => prepareAndroidConfig(root, {}), /SYNTHETIC_CONFIG_IS_CI_ONLY/);
    assert.equal(fs.existsSync(path.join(root, 'app', 'google-services.json')), false);
});

test('fresh CI configuration supplies package identifiers and the required web OAuth resource', t => {
    const root = workspace(t);
    const target = prepareAndroidConfig(root, { GITHUB_ACTIONS: 'true' });
    const config = JSON.parse(fs.readFileSync(target, 'utf8'));
    assert.equal(config.project_info.project_id, 'demo-financeai-ci');
    assert.equal(config.client[0].client_info.android_client_info.package_name, 'com.ahmetkaragunlu.financeai');
    assert.equal(config.client[0].oauth_client[0].client_type, 3);
    assert.equal(config.client[0].oauth_client[0].client_id, '1234567890-ci.apps.googleusercontent.com');
    assert.equal(config.client[0].api_key[0].current_key, 'ci-firebase-placeholder');
    const original = fs.readFileSync(target, 'utf8');
    assert.throws(() => prepareAndroidConfig(root, { GITHUB_ACTIONS: 'true' }), /EXISTING_LOCAL_CONFIG/);
    assert.equal(fs.readFileSync(target, 'utf8'), original);
});

test('existing local properties are preserved and prohibit synthetic configuration', t => {
    const root = workspace(t), properties = path.join(root, 'local.properties');
    fs.writeFileSync(properties, 'protected-local-configuration\n');
    assert.throws(() => prepareAndroidConfig(root, { GITHUB_ACTIONS: 'true' }), /EXISTING_LOCAL_CONFIG/);
    assert.equal(fs.readFileSync(properties, 'utf8'), 'protected-local-configuration\n');
    assert.equal(fs.existsSync(path.join(root, 'app', 'google-services.json')), false);
});

test('a linked existing config is not overwritten or followed', t => {
    const root = workspace(t), original = path.join(root, 'protected.json');
    fs.writeFileSync(original, 'protected-configuration\n');
    fs.symlinkSync(original, path.join(root, 'app', 'google-services.json'));
    assert.throws(() => prepareAndroidConfig(root, { GITHUB_ACTIONS: 'true' }), /EXISTING_LOCAL_CONFIG/);
    assert.equal(fs.readFileSync(original, 'utf8'), 'protected-configuration\n');
});
