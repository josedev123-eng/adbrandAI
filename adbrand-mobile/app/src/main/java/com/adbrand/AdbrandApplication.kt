package com.adbrand

import android.app.Application
import com.adbrand.core.session.GestorSesion

// HU 42: la sesión cifrada se prepara una sola vez, antes de abrir cualquier pantalla.
class AdbrandApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        GestorSesion.inicializar(this)
    }
}
