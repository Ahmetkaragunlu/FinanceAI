package com.ahmetkaragunlu.financeai.firebaseRepo

import com.ahmetkaragunlu.financeai.model.User
import com.google.firebase.auth.AuthResult

interface AuthRepository {
    val currentUser: Any?
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun saveUser(email: String, firstName: String, lastName: String, password: String)
    suspend fun saveUserFirestore(user: User)
    fun logOut()
}