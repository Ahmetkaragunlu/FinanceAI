import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

interface GoogleSignInRequest {
  idToken: string;
}

export const signInWithGoogle = functions.https.onCall(async (request) => {
  const {idToken} = request.data as GoogleSignInRequest;

  // 1️⃣ ID token kontrolü
  if (!idToken) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "ID token required"
    );
  }

  // 2️⃣ Google token doğrulama
  let decodedToken;
  try {
    decodedToken = await admin.auth().verifyIdToken(idToken);
  } catch (e: any) {
    console.error("Token verification failed:", e);
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Invalid or expired ID token"
    );
  }

  const email = decodedToken.email;
  const uid = decodedToken.uid;

  if (!email) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Email not found in token"
    );
  }

  try {
    // 3️⃣ Firestore'da kullanıcı kontrolü
    const userQuery = await admin.firestore()
      .collection("users")
      .where("email", "==", email)
      .limit(1)
      .get();

    if (userQuery.empty) {
      // Kullanıcı sistemde yok → client'a doğru hata
      throw new functions.https.HttpsError(
        "permission-denied",
        "User not registered",
        {userNotRegistered: true}
      );
    }

    // 4️⃣ Auth sisteminde kullanıcı kontrolü
    try {
      await admin.auth().getUser(uid);
    } catch {
      throw new functions.https.HttpsError(
        "permission-denied",
        "User not registered in Auth",
        {userNotRegistered: true}
      );
    }

    // 5️⃣ Custom token oluştur
    const customToken = await admin.auth().createCustomToken(uid);

    // 6️⃣ Son giriş zamanını güncelle (opsiyonel)
    await userQuery.docs[0].ref.update({
      lastSignIn: admin.firestore.FieldValue.serverTimestamp(),
    });

    // 7️⃣ Başarılı giriş → sadece custom token dön
    return {customToken};
  } catch (error) {
    // 8️⃣ HttpsError ise client'a olduğu gibi dön
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    // Beklenmedik hata → internal
    console.error("Unexpected Google sign-in error:", error);
    throw new functions.https.HttpsError(
      "internal",
      "Internal server error"
    );
  }
});
