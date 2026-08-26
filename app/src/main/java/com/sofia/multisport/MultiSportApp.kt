package com.sofia.multisport

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class MultiSportApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // Suscribir a todos los usuarios al tema "partidos" para recibir avisos globales
        FirebaseMessaging.getInstance().subscribeToTopic("partidos")
    }
}
