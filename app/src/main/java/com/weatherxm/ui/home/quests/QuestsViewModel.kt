package com.weatherxm.ui.home.quests

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class QuestsViewModel() : ViewModel() {
    var user: FirebaseUser? = null
}
