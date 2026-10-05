package com.ahmetkaragunlu.financeai.app.presentation

import androidx.lifecycle.ViewModel
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(session: AccountSession) : ViewModel() {
    val account = session.account
}
