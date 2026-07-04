package com.chambita.app.ui.auth

import android.content.Intent
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chambita.app.R
import com.chambita.app.data.local.AppDatabase
import kotlinx.coroutines.launch

abstract class NavActivity : AppCompatActivity() {

    protected fun barraNavegacion() {
        lifecycleScope.launch {
            val dbLocal = AppDatabase.getDatabase(this@NavActivity)
            val session = dbLocal.userSessionDao().getActiveSession()
            val rol = session?.rol ?: "cliente"

            findViewById<LinearLayout>(R.id.navInicio)?.setOnClickListener {
                val targetActivity = if (rol == "tecnico") HomeTecnicoActivity::class.java else HomeClienteActivity::class.java
                if (this@NavActivity.javaClass != targetActivity) {
                    val intent = Intent(this@NavActivity, targetActivity)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                }
            }

            findViewById<LinearLayout>(R.id.navPerfil)?.setOnClickListener {
                val targetActivity = if (rol == "tecnico") PerfilTecnicoActivity::class.java else PerfilClienteActivity::class.java
                if (this@NavActivity.javaClass != targetActivity) {
                    val intent = Intent(this@NavActivity, targetActivity)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                }
            }

            findViewById<LinearLayout>(R.id.navTrabajos)?.setOnClickListener {
                if (this@NavActivity.javaClass != MisSolicitudesActivity::class.java) {
                    val intent = Intent(this@NavActivity, MisSolicitudesActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                }
            }

            findViewById<LinearLayout>(R.id.navMensajes)?.setOnClickListener {
                if (this@NavActivity.javaClass != MensajesActivity::class.java) {
                    val intent = Intent(this@NavActivity, MensajesActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                }
            }
        }
    }

    protected fun showToast(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}
