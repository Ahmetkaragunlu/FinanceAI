'use strict';

// Models atomic commit/rollback and prohibits reads after writes. Not a Rules/IAM emulator.
function firestoreMemory(seed) {
    const values = new Map(Object.entries(seed).map(([key, value]) => [key, structuredClone(value)]));
    const writes = [];
    let queue = Promise.resolve();
    const collection = name => ({ ...query(name), doc: id => {
        if (typeof id !== 'string' || !id || id.includes('/')) throw new Error('INVALID_DOCUMENT_ID');
        return { id, path: name + '/' + id, get: async () => ({ exists: values.has(name + '/' + id),
            data: () => structuredClone(values.get(name + '/' + id)) }) };
    } });
    function query(name, filters = [], maximum = Infinity) {
        return {
            where: (field, operator, value) => query(name, [...filters, [field, operator, value]], maximum),
            limit: size => query(name, filters, size),
            get: async () => {
                const docs = [...values.entries()].filter(([key, value]) => key.startsWith(name + '/') &&
                    filters.every(([field, operator, expected]) => operator === '==' && value[field] === expected)).slice(0, maximum);
                return { empty: docs.length === 0, size: docs.length };
            }
        };
    }
    function runTransaction(block) {
        const run = queue.then(async () => {
            const pending = [];
            const tx = {
                get: async ref => {
                    if (pending.length) throw new Error('READ_AFTER_WRITE');
                    const value = values.get(ref.path);
                    return { exists: value !== undefined, data: () => structuredClone(value), id: ref.id };
                },
                set: (ref, value) => pending.push({ path: ref.path, value: structuredClone(value), merge: false }),
                update: (ref, value) => pending.push({ path: ref.path, value: structuredClone(value), merge: true })
            };
            await block(tx);
            for (const change of pending) {
                if (change.merge && !values.has(change.path)) throw new Error('MISSING_UPDATE');
                values.set(change.path, change.merge ? { ...values.get(change.path), ...change.value } : change.value);
                writes.push(change.path);
            }
        });
        queue = run.catch(() => {});
        return run;
    }
    return { collection, runTransaction, values, writes };
}
module.exports = { firestoreMemory };
