package com.ahmetkaragunlu.financeai

import com.ahmetkaragunlu.financeai.app.MainActivity as AppMainActivity
import dagger.hilt.android.AndroidEntryPoint

/** Keeps the existing launcher and explicit PendingIntent component identity stable. */
@AndroidEntryPoint
class MainActivity : AppMainActivity()
