'use strict';

const { before, beforeEach, after, test } = require('node:test');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const { strict: assert } = require('node:assert');
const { initializeTestEnvironment, assertSucceeds, assertFails } = require('@firebase/rules-unit-testing');
const { ref, uploadBytes, getBytes, getMetadata, getDownloadURL, updateMetadata, deleteObject, listAll } = require('firebase/storage');

const bucket = 'gs://demo-financeai.firebasestorage.app';
const currentPath = 'users/A/transactions/record-1/version-1.jpg';
const legacyPath = 'users/A/scheduled/record-2_photo.jpg';
const jpeg = new Uint8Array([0xff, 0xd8, 0xff, 0xd9]);
const metadata = { contentType: 'image/jpeg' };
let environment;

before(async () => {
    if (!process.env.FIREBASE_STORAGE_EMULATOR_HOST) throw new Error('LOCAL_STORAGE_EMULATOR_REQUIRED');
    environment = await initializeTestEnvironment({ projectId: 'demo-financeai',
        storage: { rules: readFileSync(path.resolve(__dirname, '../../../storage.rules'), 'utf8') } });
});
beforeEach(async () => {
    await environment.clearStorage();
    await environment.withSecurityRulesDisabled(async context => {
        const storage = context.storage(bucket);
        await uploadBytes(ref(storage, currentPath), jpeg, metadata);
        // Preserve access to old metadata too: get/delete must not depend on new upload validation.
        await uploadBytes(ref(storage, legacyPath), jpeg, { contentType: 'application/octet-stream' });
        await uploadBytes(ref(storage, 'unowned/old.jpg'), jpeg, metadata);
    });
});
after(async () => { if (environment) await environment.cleanup(); });
const storage = uid => (uid ? environment.authenticatedContext(uid) : environment.unauthenticatedContext()).storage(bucket);
const upload = (uid, path, value = jpeg, info = metadata) => uploadBytes(ref(storage(uid), path), value, info);

test('signed-out clients cannot download, upload, overwrite, list or delete account photos', async () => {
    const client = storage();
    await assertFails(getBytes(ref(client, currentPath)));
    await assertFails(getMetadata(ref(client, currentPath)));
    await assertFails(getDownloadURL(ref(client, currentPath)));
    await assertFails(upload(undefined, 'users/A/transactions/record-1/new.jpg'));
    await assertFails(upload(undefined, currentPath));
    await assertFails(deleteObject(ref(client, currentPath)));
    await assertFails(listAll(ref(client, 'users/A/transactions')));
});

test('another account cannot download, overwrite, forge ownership metadata or delete a photo', async () => {
    const client = storage('B');
    await assertFails(getBytes(ref(client, currentPath)));
    await assertFails(getDownloadURL(ref(client, currentPath)));
    await assertFails(upload('B', currentPath, jpeg, { ...metadata, customMetadata: { ownerId: 'B' } }));
    await assertFails(updateMetadata(ref(client, currentPath), { customMetadata: { ownerId: 'B' } }));
    await assertFails(deleteObject(ref(client, currentPath)));
    await assertSucceeds(upload('B', 'users/B/transactions/record-1/version-1.jpg'));
});

test('the owner can create, download, retry and delete both current collection paths', async () => {
    for (const collection of ['transactions', 'scheduled']) {
        const path = `users/A/${collection}/record-new/version-new.jpg`;
        await assertSucceeds(upload('A', path));
        const reference = ref(storage('A'), path);
        const contents = await assertSucceeds(getBytes(reference));
        assert.deepEqual(new Uint8Array(contents), jpeg);
        await assertSucceeds(getDownloadURL(reference));
        await assertSucceeds(upload('A', path));
        await assertSucceeds(deleteObject(reference));
    }
});

test('separate devices authenticated to the same UID share access, not device identities', async () => {
    const firstDevice = environment.authenticatedContext('A').storage(bucket);
    const secondDevice = environment.authenticatedContext('A').storage(bucket);
    const path = 'users/A/scheduled/shared-plan/shared-version.jpg';
    await assertSucceeds(uploadBytes(ref(firstDevice, path), jpeg, metadata));
    await assertSucceeds(getBytes(ref(secondDevice, path)));
    await assertSucceeds(uploadBytes(ref(secondDevice, path), jpeg, metadata));
    await assertSucceeds(deleteObject(ref(secondDevice, path)));
});

test('legacy flat paths retain owned read, retry and deletion, including old content types', async () => {
    await assertSucceeds(getBytes(ref(storage('A'), legacyPath)));
    await assertFails(getBytes(ref(storage('B'), legacyPath)));
    await assertFails(deleteObject(ref(storage('B'), legacyPath)));
    await assertSucceeds(deleteObject(ref(storage('A'), legacyPath)));
    for (const collection of ['transactions', 'scheduled']) {
        const path = `users/A/${collection}/legacy-record_photo.jpg`;
        await assertSucceeds(upload('A', path));
        await assertSucceeds(upload('A', path));
        await assertSucceeds(getBytes(ref(storage('A'), path)));
        await assertSucceeds(deleteObject(ref(storage('A'), path)));
    }
});

test('clients cannot enumerate the bucket or upload to unknown, unowned or deeper folders', async () => {
    const client = storage('A');
    await assertFails(listAll(ref(client)));
    await assertFails(listAll(ref(client, 'users/A/transactions')));
    await assertFails(getBytes(ref(client, 'unowned/old.jpg')));
    for (const path of ['unowned/new.jpg', 'users/A/avatars/record/new.jpg',
        'users/A/transactions/random.jpg', 'users/A/transactions/record/nested/new.jpg',
        'users/B/scheduled/record/new.jpg']) {
        await assertFails(upload('A', path));
    }
});

test('new uploads require JPEG metadata, a JPEG filename and nonempty content; deletes remain valid', async () => {
    await assertFails(upload('A', 'users/A/transactions/record-new/photo.png', jpeg, { contentType: 'image/png' }));
    await assertFails(upload('A', 'users/A/transactions/record-new/photo.jpg', jpeg, { contentType: 'application/octet-stream' }));
    await assertFails(upload('A', 'users/A/transactions/record-new/photo.jpg', new Uint8Array()));
    await assertFails(updateMetadata(ref(storage('A'), currentPath), { contentType: 'text/html' }));
    await assertSucceeds(upload('A', 'users/A/transactions/record-new/photo.jpg', jpeg, { contentType: 'image/jpg' }));
    await assertSucceeds(deleteObject(ref(storage('A'), currentPath)));
});
