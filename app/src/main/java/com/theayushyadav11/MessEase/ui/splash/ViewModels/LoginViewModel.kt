package com.theayushyadav11.MessEase.ui.splash.ViewModels

import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.firestore
import com.theayushyadav11.MessEase.utils.Constants.Companion.USERS

class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val databaeReference = FirebaseDatabase.getInstance().reference
    private val firestoreReference = Firebase.firestore

    fun isPresent(onSuccess: (Boolean) -> Unit) {
        firestoreReference.collection(USERS).document(auth.currentUser?.uid.toString()).get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    if (it.result?.exists() == true) {
                        onSuccess(true)
                    } else {
                        onSuccess(false)
                    }
                } else {
                    onSuccess(false)
                }
            }
    }
}
