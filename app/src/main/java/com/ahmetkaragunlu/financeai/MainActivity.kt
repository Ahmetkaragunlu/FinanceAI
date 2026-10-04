package com.ahmetkaragunlu.financeai

import dagger.hilt.android.AndroidEntryPoint

/** Keeps the existing launcher and explicit PendingIntent component identity stable. */
@AndroidEntryPoint
class MainActivity : com.ahmetkaragunlu.financeai.app.MainActivity()
