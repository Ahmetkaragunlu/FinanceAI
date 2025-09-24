package com.ahmetkaragunlu.financeai.firebaseRepo

import com.ahmetkaragunlu.financeai.model.User
import com.ahmetkaragunlu.financeai.screens.auth.AuthException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUser get() = auth.currentUser

    override suspend fun signUp(email: String, password: String): AuthResult =
        auth.createUserWithEmailAndPassword(email, password).await()

    override suspend fun signIn(email: String, password: String): AuthResult =
        auth.signInWithEmailAndPassword(email, password).await()

    override suspend fun logOut() {
        auth.signOut()
    }

    override suspend fun saveUserFirestore(user: User) {
        firestore.collection("users").document(user.uid).set(user).await()
    }

    override suspend fun saveUser(email: String, firstName: String, lastName: String, password: String) {
        val nameQuery = firestore.collection("users")
            .whereEqualTo("firstName", firstName)
            .whereEqualTo("lastName", lastName)
            .get().await()

        if (!nameQuery.isEmpty) {
            throw AuthException.NameExists
        }

        val authResult = try {
            signUp(email = email, password = password)
        } catch (e: FirebaseAuthUserCollisionException) {
            throw AuthException.EmailExists
        }

        val uid = authResult.user?.uid?: throw AuthException.UidNotFound
        val user = User(firstName = firstName, lastName = lastName, email = email, uid = uid)
        saveUserFirestore(user)
    }
}
