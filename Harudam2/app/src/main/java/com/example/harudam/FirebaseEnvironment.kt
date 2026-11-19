package com.example.harudam

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions

object FirebaseEnvironment {
    private var configured = false
    @Synchronized fun configure() {
        if(configured) return
        if(BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS) {
            val host = BuildConfig.FIREBASE_EMULATOR_HOST
            FirebaseAuth.getInstance().useEmulator(host, 9099)
            FirebaseFirestore.getInstance().useEmulator(host, 8080)
            FirebaseFunctions.getInstance("asia-northeast3").useEmulator(host, 5001)
        }
        configured = true
    }
}
