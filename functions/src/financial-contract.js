'use strict';

// Persisted field names, not an automatic allowlist of fields editable during completion.
const FinancialFields = Object.freeze({
    AMOUNT_MINOR: 'amountMinor',
    LEGACY_AMOUNT: 'amount',
    CURRENCY_CODE: 'currencyCode',
    TRANSACTION: 'transaction',
    CATEGORY: 'category',
    NOTE: 'note',
    LOCATION_FULL: 'locationFull',
    LOCATION_SHORT: 'locationShort',
    LATITUDE: 'latitude',
    LONGITUDE: 'longitude',
    PHOTO_STORAGE_URL: 'photoStorageUrl',
    PHOTO_REMOVED: 'photoRemoved',
    PHOTO_VERSION: 'photoVersion',
    PHOTO_INTENT: 'photoIntent'
});

module.exports = { FinancialFields };
