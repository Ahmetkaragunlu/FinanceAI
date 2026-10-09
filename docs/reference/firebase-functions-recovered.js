const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

// ============================================
// 1. Yeni scheduled transaction oluşturulduğunda (İLK KEZ)
// ============================================
exports.sendScheduledNotification = functions.firestore
    .document('scheduled_transactions/{transactionId}')
    .onCreate(async (snap, context) => {

        const transactionId = context.params.transactionId;
        const transaction = snap.data();
        const userId = transaction.userId;
        const scheduledDate = transaction.scheduledDate;
        const currentTime = Date.now();

        if (scheduledDate < currentTime) {
            return null;
        }

        const userDoc = await admin.firestore().collection('users').doc(userId).get();
        if (!userDoc.exists) {
            return null;
        }

        const fcmTokens = userDoc.data().fcmTokens || [];
        if (fcmTokens.length === 0) {
            return null;
        }


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
        }

        return null;
    });

// ============================================
// 2. Manuel tetikleme - 15 dakikalık periyodik bildirimler
// ============================================
exports.manualSendToAllDevices = functions.firestore
    .document('notification_triggers/{triggerId}')
    .onCreate(async (snap, context) => {

        const triggerData = snap.data();
        const transactionId = triggerData.transactionId;

        try {
            const scheduledDoc = await admin.firestore()
                .collection('scheduled_transactions')
                .doc(transactionId)
                .get();

            if (!scheduledDoc.exists) {
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
                await snap.ref.delete();
                return null;
            }

            const fcmTokens = userDoc.data().fcmTokens || [];
            if (fcmTokens.length === 0) {
                await snap.ref.delete();
                return null;
            }


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
                    console.error('Notification send failed');
                    return { success: false };
                }
            });

            await Promise.all(sendPromises);
            await snap.ref.delete();
            
            return null;
        } catch (error) {
            console.error('Manual notification processing failed');
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

        const transactionId = context.params.transactionId;
        const transaction = snap.data();
        const userId = transaction.userId;

        const userDoc = await admin.firestore().collection('users').doc(userId).get();
        if (!userDoc.exists) {
            return null;
        }

        const fcmTokens = userDoc.data().fcmTokens || [];
        if (fcmTokens.length === 0) {
            return null;
        }


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
        return null;
    });

// ============================================
// 4. (DEPRECATED) HAYIR butonuna basıldığında
// ============================================
exports.onNotificationDismissed = functions.firestore
    .document('notification_dismissals/{dismissalId}')
    .onCreate(async (snap, context) => {
        await snap.ref.delete();
        return null;
    });

// ============================================
// 5. HAYIR butonu - 15dk reschedule VE BİLDİRİMİ KAPAT
// ============================================
exports.scheduleReminderTask = functions.firestore
    .document('notification_reminders/{reminderId}')
    .onCreate(async (snap, context) => {
        
        const reminderData = snap.data();
        const transactionId = reminderData.transactionId;
        const userId = reminderData.userId;

        
        try {
            if (!userId || !transactionId) {
                return null;
            }

            // DISMISS mesajı gönder (tüm cihazlarda bildirimi kapat)
            const userDoc = await admin.firestore().collection('users').doc(userId).get();
            if (!userDoc.exists) {
                return null;
            }

            const fcmTokens = userDoc.data().fcmTokens || [];
            if (fcmTokens.length === 0) {
                return null;
            }


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
            
            return null;

        } catch (error) {
            console.error('Reminder scheduling failed');
            return null;
        }
    });

// ============================================
// 6. Uygulama açıldığında bekleyen bildirimleri gönder
//    🔥 ÖNEMLİ: WAITING reminder'lı işlemler ATLANIR
// ============================================
exports.sendPendingNotifications = functions.https.onCall(async (data, context) => {

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
                remindersToDelete.push(doc.ref);
                return;
            }

            if (triggerTime > currentTime) {
                // ⏳ WAITING: Kullanıcı HAYIR dedi ve 15dk henüz dolmadı
                transactionReminderStatus.set(transactionId, { status: 'WAITING' });
            } else {
                // ⏱ EXPIRED: 15dk doldu, reschedule gerekli
                transactionReminderStatus.set(transactionId, { status: 'EXPIRED' });
                remindersToDelete.push(doc.ref);
                if (!transactionsToReschedule.has(transactionId)) {
                    transactionsToReschedule.set(transactionId, { transactionId, userId });
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
                            console.error('Reschedule send failed');
                        }
                    }
                }
            }
        }
        
        // ============================================================
        // 3) ESKİ REMINDER'LARI SİL
        // ============================================================
        if (remindersToDelete.length > 0) {
            const batch = admin.firestore().batch();
            remindersToDelete.forEach(ref => batch.delete(ref));
            await batch.commit();
        }

        // ============================================================
        // 4) "HESABA YENİ GİRDİM" BİLDİRİMLERİ
        //    🔥 ÖNEMLİ: WAITING reminder'lı işlemler ATLANIR!
        // ============================================================
        const scheduledSnapshot = await admin.firestore()
            .collection('scheduled_transactions')
            .where('userId', '==', userId)
            .get();


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
                continue;
            }

            // Reminder durumunu kontrol et
            const reminderStatus = transactionReminderStatus.get(transactionId);

            if (!reminderStatus) {
                // ✅ Reminder yok → BİLDİRİM GÖNDERİLEBİLİR
                pendingTransactions.push({ doc, data: d });
            } else if (reminderStatus.status === 'WAITING') {
                // 🔥 ÇÖZÜM: WAITING reminder varsa BİLDİRİM GÖNDERİLMEZ!
            } else if (reminderStatus.status === 'EXPIRED') {
                // ⏭️ EXPIRED: RESCHEDULE zaten gönderildi, bildirim gerekmez
            }
        }

        // ============================================================
        // 5) BİLDİRİM GÖNDER (SADECE BU CİHAZA)
        // ============================================================
        if (pendingTransactions.length === 0) {
            return { success: true, count: 0 };
        }


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
            } catch (err) {
                console.error('Pending notification send failed');
            }
        }

        return { success: true, count: sent };

    } catch (error) {
        console.error('Pending notification processing failed');
        throw new functions.https.HttpsError('internal', 'Error sending notifications');
    }
});

// ============================================
// 7. OTOMATİK ZAMANLAYICI - Süresi dolan reminder'ları kontrol et
// ============================================
exports.checkExpiredReminders = functions.pubsub.schedule('every 5 minutes')
    .onRun(async (context) => {
        const currentTime = Date.now();

        try {
            const expiredRemindersSnapshot = await admin.firestore()
                .collection('notification_reminders')
                .where('triggerTime', '<=', currentTime)
                .get();

            if (expiredRemindersSnapshot.isEmpty) {
                return null;
            }


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
                            console.error('Reschedule send failed');
                        }
                    }
                }
            }

            // Reminder'ları sil
            if (remindersToDelete.length > 0) {
                const batch = admin.firestore().batch();
                remindersToDelete.forEach(ref => batch.delete(ref));
                await batch.commit();
            }

            return null;

        } catch (error) {
            console.error('Expired reminder processing failed');
            return null;
        }
    });
