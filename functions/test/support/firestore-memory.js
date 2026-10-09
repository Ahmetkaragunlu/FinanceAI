'use strict';

// Models atomic commit/rollback and prohibits reads after writes. Not a Rules/IAM emulator.
function firestoreMemory(seed) {
    const values = new Map(Object.entries(seed).map(([key, value]) => [key, structuredClone(value)]));
    const writes = [];
    const queries = [];
    let queue = Promise.resolve();
    const reference = (name, id) => {
        if (typeof id !== 'string' || !id || id.includes('/')) throw new Error('INVALID_DOCUMENT_ID');
        const path = name + '/' + id;
        const ref = { id, path,
            get: async () => {
                const value = structuredClone(values.get(path));
                return { id, ref, exists: value !== undefined, data: () => structuredClone(value) };
            },
            update: async patch => {
                if (!values.has(path)) throw new Error('MISSING_UPDATE');
                values.set(path, { ...values.get(path), ...structuredClone(patch) });
                writes.push(path);
            },
            delete: async () => { values.delete(path); writes.push(path); }
        };
        return ref;
    };
    const collection = name => ({ ...query(name), doc: id => reference(name, id) });
    // Only the query shapes used by these handlers: equality/due filters and document-ID cursors.
    function query(name, filters = [], maximum = Infinity, ordered = false, cursor = null) {
        return {
            where: (field, operator, value) => {
                if (!['==', '<='].includes(operator)) throw new Error('UNSUPPORTED_TEST_QUERY');
                return query(name, [...filters, [field, operator, value]], maximum, ordered, cursor);
            },
            limit: size => query(name, filters, size, ordered, cursor),
            orderBy: () => query(name, filters, maximum, true, cursor),
            startAfter: id => query(name, filters, maximum, ordered, id),
            get: async () => {
                queries.push({ name, filters: structuredClone(filters), maximum, ordered, cursor });
                let rows = [...values.entries()].filter(([key, value]) => key.startsWith(name + '/') &&
                    filters.every(([field, operator, expected]) => operator === '==' ? value[field] === expected :
                        typeof value[field] === 'number' && value[field] <= expected));
                if (ordered) rows.sort(([a], [b]) => a < b ? -1 : a > b ? 1 : 0);
                rows = rows.filter(([key]) => !cursor || key.slice(name.length + 1) > cursor).slice(0, maximum);
                const docs = rows.map(([key]) => {
                    const id = key.slice(name.length + 1), ref = reference(name, id);
                    const value = structuredClone(values.get(key));
                    return { id, ref, exists: true, data: () => structuredClone(value) };
                });
                return { empty: docs.length === 0, size: docs.length, docs };
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
                update: (ref, value) => pending.push({ path: ref.path, value: structuredClone(value), merge: true }),
                create: (ref, value) => pending.push({ path: ref.path, value: structuredClone(value), create: true })
            };
            await block(tx);
            const committed = new Map(values);
            for (const change of pending) {
                if (change.merge && !committed.has(change.path)) throw new Error('MISSING_UPDATE');
                if (change.create && committed.has(change.path)) throw new Error('DOCUMENT_EXISTS');
                committed.set(change.path, change.merge ? { ...committed.get(change.path), ...change.value } : change.value);
            }
            for (const change of pending) { values.set(change.path, committed.get(change.path)); writes.push(change.path); }
        });
        queue = run.catch(() => {});
        return run;
    }
    return { collection, runTransaction, values, writes, queries };
}
module.exports = { firestoreMemory };
