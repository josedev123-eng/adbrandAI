package com.adbrand.core.session

// HU 42: contrato mínimo para que las pruebas no dependan de Android/EncryptedSharedPreferences.
interface AlmacenSesion {

    fun guardarSesion(tokenAcceso: String, tokenRenovacion: String?)

    fun tokenAcceso(): String?

    fun tokenRenovacion(): String?

    fun haySesion(): Boolean

    fun limpiar()
}
