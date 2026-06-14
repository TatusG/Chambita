package com.chambita.app.ui.auth

import android.content.Intent
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chambita.app.R

abstract class NavActivity : AppCompatActivity() {

    @Suppress("unused")
    protected fun barraNavegacion() {

        findViewById<LinearLayout>(R.id.navInicio)?.setOnClickListener {
            if (this.javaClass != HomeClienteActivity::class.java) {
                startActivity(Intent(this, HomeClienteActivity::class.java))
                finish()
            }
        }

        findViewById<LinearLayout>(R.id.navPerfil)?.setOnClickListener {
            if (this.javaClass != PerfilClienteActivity::class.java ) {
                startActivity(Intent(this, PerfilClienteActivity::class.java))
                finish()
            }
        }

        findViewById<LinearLayout>(R.id.navTrabajos)?.setOnClickListener {
            showToast("Próximamente: Trabajos")
        }

        findViewById<LinearLayout>(R.id.navMensajes)?.setOnClickListener {
            showToast("Próximamente: Mensajes")
        }
    }

    protected fun showToast(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}