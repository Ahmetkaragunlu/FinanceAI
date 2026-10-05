const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

// ============================================
// 1. Yeni scheduled transaction oluşturulduğunda (İLK KEZ)
// ============================================
exports.sendScheduledNotification = functions.firestore
    .document('scheduled_transactions/{transactionId}')
    .onCreate(async (snap, context) => {
        console.log('🔥 FUNCTION: sendScheduledNotification (onCreate)');

        const transactionId = context.params.transactionId;
        const transaction = snap.data();
        const userId = transaction.userId;
        const scheduledDate = transaction.scheduledDate;
        const currentTime = Date.now();

        if (scheduledDate < currentTime) {
            console.log('⏭️ Scheduled date in the past, skipping');
            return null;
        }

        const userDoc = await admin.firestore().collection('users').doc(userId).get();
        if (!userDoc.exists) {
            console.log('⚠️ User not found');
            return null;
        }

        const fcmTokens = userDoc.data().fcmTokens || [];
        if (fcmTokens.length === 0) {
            console.log('⚠️ No FCM tokens (user not logged in on any device)');
            return null;
        }

        console.log(`📡 Sending to ${fcmTokens.length} devices`);

        const sendPromises = fcmTokens.map(async (token) => {
            try {
                await admin.messaging().send({
                    token: token,
                    data: {
                        type: 'SCHEDULED_REMINDER',
                        transactionId: transactionId,
                        userId: userId, // ✅ EKLENDI
                        amount: transaction.amount.toString(),
                        transactionType: transaction.type,
                        category: transaction.category,
                        scheduledDate: scheduledDate.toString(),
                        updateExisting: 'false'
                    },
                    android: { priority: 'high' }
                });
                return { success: true, token: token };
            } catch (error) {
                if (error.code === 'messaging/invalid-registration-token' ||
                    error.code === 'messaging/registration-token-not-registered') {
                    return { success: false, token: token, remove: true };
                }
                return { success: false, token: token, remove: false };
            }
        });

        const results = await Promise.all(sendPromises);
        const tokensToRemove = results.filter(r => r.remove).map(r => r.token);

        if (tokensToRemove.length > 0) {
            await admin.firestore().collection('users').doc(userId).update({
                fcmTokens: admin.firestore.FieldValue.arrayRemove(...tokensToRemove)
            });
            console.log(`🗑️ Removed ${tokensToRemove.length} invalid tokens`);
        }

        console.log('✅ Initial notifications sent');
        return null;
    });

// ============================================
// 2. Manuel tetikleme - 15 dakikalık periyodik bildirimler
// ============================================
exports.manualSendToAllDevices = functions.firestore
    .document('notification_triggers/{triggerId}')
    .onCreate(async (snap, context) => {
        console.log('🔥 FUNCTION: manualSendToAllDevices');

        const triggerData = snap.data();
        const transactionId = triggerData.transactionId;

        try {
            const scheduledDoc = await admin.firestore()
                .collection('scheduled_transactions')
                .doc(transactionId)
                .get();

            if (!scheduledDoc.exists) {
                console.log('⚠️ Transaction not found, deleting trigger');
                await snap.ref.delete();
                return null;
            }

            const transaction = scheduledDoc.data();
            const userId = transaction.userId;

            const userDoc = await admin.firestore()
                .collection('users')
                .doc(userId)
                .get();

            if (!userDoc.exists) {
                console.log('⚠️ User not found');
                await snap.ref.delete();
                return null;
            }

            const fcmTokens = userDoc.data().fcmTokens || [];
            if (fcmTokens.length === 0) {
                console.log('⚠️ No FCM tokens');
                await snap.ref.delete();
                return null;
            }

            console.log(`📡 Sending to ${fcmTokens.length} devices`);

            const sendPromises = fcmTokens.map(async (token) => {
                try {
                    await admin.messaging().send({
                        token: token,
                        data: {
                            type: 'SCHEDULED_REMINDER',
                            transactionId: transactionId,
                            userId: userId, // ✅ EKLENDI
                            amount: transaction.amount.toString(),
                            transactionType: transaction.type,
                            category: transaction.category,
                            scheduledDate: transaction.scheduledDate.toString(),
                            updateExisting: 'true'
                        },
                        android: { priority: 'high' }
                    });
                    return { success: true };
                } catch (error) {
                    console.error('Error sending to token:', error);
                    return { success: false };
                }
            });

            await Promise.all(sendPromises);
            await snap.ref.delete();
            
            console.log('✅ Notifications sent to all devices');
            return null;
        } catch (error) {
            console.error('Error:', error);
            await snap.ref.delete();
            return null;
        }
    });

// ============================================
// 3. EVET butonuna basıldığında - Transaction silindi
// ============================================
exports.onScheduledTransactionDelete = functions.firestore
    .document('scheduled_transactions/{transactionId}')
    .onDelete(async (snap, context) => {
        console.log('🔥 FUNCTION: onScheduledTransactionDelete');

        const transactionId = context.params.transactionId;
        const transaction = snap.data();
        const userId = transaction.userId;

        const userDoc = await admin.firestore().collection('users').doc(userId).get();
        if (!userDoc.exists) {
            console.log('⚠️ User not found');
            return null;
        }

        const fcmTokens = userDoc.data().fcmTokens || [];
        if (fcmTokens.length === 0) {
            console.log('⚠️ No FCM tokens');
            return null;
        }

        console.log(`📡 Sending CANCEL to ${fcmTokens.length} devices`);

        const sendPromises = fcmTokens.map(async (token) => {
            try {
                await admin.messaging().send({
                    token: token,
                    data: {
                        type: 'CANCEL_NOTIFICATION',
                        transactionId: transactionId,
                        userId: userId // ✅ EKLENDI
                    },
                    android: { priority: 'high' }
                });
                return { success: true };
            } catch (error) {
                return { success: false };
            }
        });

        await Promise.all(sendPromises);
        console.log('✅ CANCEL sent to all devices');
        return null;
    });

// ============================================
// 4. (DEPRECATED) HAYIR butonuna basıldığında
// ============================================
exports.onNotificationDismissed = functions.firestore
    .document('notification_dismissals/{dismissalId}')
    .onCreate(async (snap, context) => {
        console.log('⚠️ FUNCTION: onNotificationDismissed (DEPRECATED)');
        await snap.ref.delete();
        return null;
    });

// ============================================
// 5. HAYIR butonu - 15dk reschedule VE BİLDİRİMİ KAPAT
// ============================================
exports.scheduleReminderTask = functions.firestore
    .document('notification_reminders/{reminderId}')
    .onCreate(async (snap, context) => {
        console.log('🔥 FUNCTION: scheduleReminderTask (HAYIR tıklandı)');
        
        const reminderData = snap.data();
        const transactionId = reminderData.transactionId;
        const userId = reminderData.userId;
        const triggerTime = reminderData.triggerTime;

        if (triggerTime) {
            console.log(`⏰ Reminder will trigger at: ${new Date(triggerTime).toISOString()}`);
            console.log(`⏰ Time to wait: ${Math.round((triggerTime - Date.now()) / 1000 / 60)} minutes`);
        }
        
        try {
            if (!userId || !transactionId) {
                console.log('⚠️ UserID or TransactionID not found');
                return null;
            }

            // DISMISS mesajı gönder (tüm cihazlarda bildirimi kapat)
            const userDoc = await admin.firestore().collection('users').doc(userId).get();
            if (!userDoc.exists) {
                console.log('⚠️ User not found');
                return null;
            }

            const fcmTokens = userDoc.data().fcmTokens || [];
            if (fcmTokens.length === 0) {
                console.log('⚠️ No FCM tokens');
                return null;
            }

            console.log(`📡 Sending DISMISS to ${fcmTokens.length} devices`);

            const sendPromises = fcmTokens.map(async (token) => {
                try {
                    await admin.messaging().send({
                        token: token,
                        data: {
                            type: 'DISMISS_NOTIFICATION',
                            transactionId: transactionId,
                            userId: userId // ✅ EKLENDI
                        },
                        android: { priority: 'high' }
                    });
                    return { success: true };
                } catch (error) {
                    return { success: false };
                }
            });

            await Promise.all(sendPromises);
            console.log('✅ DISMISS sent to all devices');
            console.log('✅ Reminder document KEPT for Function 6');
            
            return null;

        } catch (error) {
            console.error('Error in scheduleReminderTask:', error);
            return null;
        }
    });

// ============================================
// 6. Uygulama açıldığında bekleyen bildirimleri gönder
//    🔥 ÖNEMLİ: WAITING reminder'lı işlemler ATLANIR
// ============================================
exports.sendPendingNotifications = functions.https.onCall(async (data, context) => {
    console.log('🔥 FUNCTION: sendPendingNotifications (v3 - Fixed)');
    console.log('═══════════════════════════════════════════════════');

    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'Must be authenticated');
    }

    const userId = context.auth.uid;
    const currentDeviceToken = data.deviceToken;
    const currentTime = Date.now();

    if (!currentDeviceToken) {
        throw new functions.https.HttpsError('invalid-argument', 'deviceToken required');
    }

    try {
        console.log(`👤 User: ${userId}`);
        console.log(`📱 Device: ${currentDeviceToken.substring(0, 10)}...`);
        console.log(`⏰ Current: ${new Date(currentTime).toISOString()}`);

        // ============================================================
        // 1) REMINDER KONTROLÜ
        // ============================================================
        const remindersSnapshot = await admin.firestore()
            .collection('notification_reminders')
            .where('userId', '==', userId)
            .get();

        const transactionReminderStatus = new Map();
        const remindersToDelete = [];
        const transactionsToReschedule = new Map(); 

        remindersSnapshot.docs.forEach(doc => {
            const r = doc.data();
            const triggerTime = r.triggerTime;
            const transactionId = r.transactionId;

            if (!triggerTime) {
                console.log(`⚠️ Reminder ${doc.id} has no triggerTime - will be deleted`);
                remindersToDelete.push(doc.ref);
                return;
            }

            if (triggerTime > currentTime) {
                // ⏳ WAITING: Kullanıcı HAYIR dedi ve 15dk henüz dolmadı
                transactionReminderStatus.set(transactionId, { status: 'WAITING' });
                console.log(`⏳ Transaction ${transactionId}: WAITING (${Math.round((triggerTime - currentTime) / 1000 / 60)} min left)`);
            } else {
                // ⏱ EXPIRED: 15dk doldu, reschedule gerekli
                transactionReminderStatus.set(transactionId, { status: 'EXPIRED' });
                remindersToDelete.push(doc.ref);
                if (!transactionsToReschedule.has(transactionId)) {
                    transactionsToReschedule.set(transactionId, { transactionId, userId });
                    console.log(`🔄 Transaction ${transactionId}: MARKED FOR RESCHEDULE`);
                }
            }
        });

        // ============================================================
        // 2) ZAMANI GELENLERİ YENİDEN KUR (RESCHEDULE)
        // ============================================================
        let allUserTokens = [];
        if (transactionsToReschedule.size > 0) {
            const userDoc = await admin.firestore().collection('users').doc(userId).get();
            if (userDoc.exists) {
                allUserTokens = userDoc.data().fcmTokens || [];
            }

            if (allUserTokens.length > 0) {
                console.log(`📡 Sending RESCHEDULE for ${transactionsToReschedule.size} txns to ${allUserTokens.length} devices`);
                
                const rescheduleList = Array.from(transactionsToReschedule.values());
                
                for (const { transactionId } of rescheduleList) {
                    for (const token of allUserTokens) {
                        try {
                            await admin.messaging().send({
                                token: token,
                                data: {
                                    type: 'RESCHEDULE_NOTIFICATION',
                                    transactionId: transactionId,
                                    userId: userId // ✅ EKLENDI
                                },
                                android: { priority: 'high' }
                            });
                        } catch (error) {
                            console.error('Error sending reschedule to token:', error);
                        }
                    }
                }
                console.log('✅ RESCHEDULE sent to all devices');
            }
        }
        
        // ============================================================
        // 3) ESKİ REMINDER'LARI SİL
        // ============================================================
        if (remindersToDelete.length > 0) {
            console.log(`🗑️ Deleting ${remindersToDelete.length} expired/invalid reminders...`);
            const batch = admin.firestore().batch();
            remindersToDelete.forEach(ref => batch.delete(ref));
            await batch.commit();
            console.log('🗑️ Reminders deleted');
        }

        // ============================================================
        // 4) "HESABA YENİ GİRDİM" BİLDİRİMLERİ
        //    🔥 ÖNEMLİ: WAITING reminder'lı işlemler ATLANIR!
        // ============================================================
        const scheduledSnapshot = await admin.firestore()
            .collection('scheduled_transactions')
            .where('userId', '==', userId)
            .get();

        console.log(`📋 Found ${scheduledSnapshot.size} total scheduled transactions`);

        const pendingTransactions = [];

        for (const doc of scheduledSnapshot.docs) {
            const d = doc.data();
            const transactionId = doc.id;
            const scheduledDate = d.scheduledDate;

            // Günün sonu kontrolü
            const endOfDay = new Date(scheduledDate);
            endOfDay.setHours(23, 59, 59, 999);
            const isNotExpired = currentTime <= endOfDay.getTime();

            if (!isNotExpired) {
                console.log(`❌ Transaction ${transactionId}: DAY EXPIRED`);
                continue;
            }

            // Reminder durumunu kontrol et
            const reminderStatus = transactionReminderStatus.get(transactionId);

            if (!reminderStatus) {
                // ✅ Reminder yok → BİLDİRİM GÖNDERİLEBİLİR
                console.log(`✅ Transaction ${transactionId}: NO REMINDER - CAN SEND`);
                pendingTransactions.push({ doc, data: d });
            } else if (reminderStatus.status === 'WAITING') {
                // 🔥 ÇÖZÜM: WAITING reminder varsa BİLDİRİM GÖNDERİLMEZ!
                console.log(`🚫 Transaction ${transactionId}: REMINDER WAITING - BLOCKED FROM SENDING`);
            } else if (reminderStatus.status === 'EXPIRED') {
                // ⏭️ EXPIRED: RESCHEDULE zaten gönderildi, bildirim gerekmez
                console.log(`⏭️ Transaction ${transactionId}: REMINDER EXPIRED - SKIP (Reschedule sent)`);
            }
        }

        // ============================================================
        // 5) BİLDİRİM GÖNDER (SADECE BU CİHAZA)
        // ============================================================
        if (pendingTransactions.length === 0) {
            console.log('✅ No pending notifications to send to this device');
            console.log('═══════════════════════════════════════════════════');
            return { success: true, count: 0 };
        }

        console.log(`📤 Sending ${pendingTransactions.length} pending notifications to THIS DEVICE`);

        let sent = 0;
        for (const { doc, data: d } of pendingTransactions) {
            const transactionId = doc.id;
            try {
                await admin.messaging().send({
                    token: currentDeviceToken,
                    data: {
                        type: 'SCHEDULED_REMINDER',
                        transactionId: transactionId,
                        userId: userId, // ✅ EKLENDI
                        amount: d.amount.toString(),
                        transactionType: d.type,
                        category: d.category,
                        scheduledDate: d.scheduledDate.toString(),
                        updateExisting: 'false'
                    },
                    android: { priority: 'high' }
                });
                sent++;
                console.log(`✅ Sent notification for ${transactionId} to current device`);
            } catch (err) {
                console.error(`❌ Failed to send for ${transactionId}:`, err.message);
            }
        }

        console.log('═══════════════════════════════════════════════════');
        return { success: true, count: sent };

    } catch (error) {
        console.error('❌ Error:', error);
        throw new functions.https.HttpsError('internal', 'Error sending notifications');
    }
});

// ============================================
// 7. OTOMATİK ZAMANLAYICI - Süresi dolan reminder'ları kontrol et
// ============================================
exports.checkExpiredReminders = functions.pubsub.schedule('every 5 minutes')
    .onRun(async (context) => {
        console.log('🔥 FUNCTION: checkExpiredReminders (Cron Job)');
        const currentTime = Date.now();

        try {
            const expiredRemindersSnapshot = await admin.firestore()
                .collection('notification_reminders')
                .where('triggerTime', '<=', currentTime)
                .get();

            if (expiredRemindersSnapshot.isEmpty) {
                console.log('✅ No expired reminders found.');
                return null;
            }

            console.log(`⏱ Found ${expiredRemindersSnapshot.size} expired reminders.`);

            const remindersToDelete = [];
            const reschedulesByUser = new Map();

            expiredRemindersSnapshot.docs.forEach(doc => {
                const r = doc.data();
                const userId = r.userId;
                const transactionId = r.transactionId;

                if (userId && transactionId) {
                    const userTasks = reschedulesByUser.get(userId) || [];
                    if (!userTasks.includes(transactionId)) {
                        userTasks.push(transactionId);
                    }
                    reschedulesByUser.set(userId, userTasks);
                }
                remindersToDelete.push(doc.ref);
            });

            // RESCHEDULE gönder
            for (const [userId, transactionIds] of reschedulesByUser.entries()) {
                console.log(`🔄 Rescheduling ${transactionIds.length} tasks for user: ${userId}`);
                
                const userDoc = await admin.firestore().collection('users').doc(userId).get();
                if (!userDoc.exists) continue;

                const fcmTokens = userDoc.data().fcmTokens || [];
                if (fcmTokens.length === 0) continue;

                for (const transactionId of transactionIds) {
                    for (const token of fcmTokens) {
                        try {
                            await admin.messaging().send({
                                token: token,
                                data: {
                                    type: 'RESCHEDULE_NOTIFICATION',
                                    transactionId: transactionId,
                                    userId: userId // ✅ EKLENDI
                                },
                                android: { priority: 'high' }
                            });
                        } catch (error) {
                            console.error('Error sending RESCHEDULE:', error);
                        }
                    }
                }
                console.log(`✅ RESCHEDULE sent for user: ${userId}`);
            }

            // Reminder'ları sil
            if (remindersToDelete.length > 0) {
                console.log(`🗑️ Deleting ${remindersToDelete.length} processed reminders...`);
                const batch = admin.firestore().batch();
                remindersToDelete.forEach(ref => batch.delete(ref));
                await batch.commit();
                console.log('🗑️ Processed reminders deleted');
            }

            return null;

        } catch (error) {
            console.error('❌ Error in checkExpiredReminders:', error);
            return null;
        }
    });
